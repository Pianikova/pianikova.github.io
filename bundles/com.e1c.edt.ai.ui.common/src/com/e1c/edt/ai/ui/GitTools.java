/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;

public class GitTools implements IGitTools
{
    @Override
    public void getDiff(Repository repository, int contextLines, OutputStream gitDiffStream)
        throws GitAPIException, IOException
    {
        try (Git git = new Git(repository); DiffFormatter diffFormatter = new DiffFormatter(gitDiffStream))
        {
            diffFormatter.setRepository(repository);
            diffFormatter.setContext(contextLines);
            // Check for commits in the repository
            var headCommitId = repository.resolve(Constants.HEAD);
            List<DiffEntry> diffs;
            if (headCommitId == null)
            {
                // Empty repository case: compare empty tree with index
                try (var reader = repository.newObjectReader())
                {
                    var headTreeIter = new EmptyTreeIterator();
                    var indexTreeIter = new DirCacheIterator(repository.readDirCache());
                    diffs = diffFormatter.scan(headTreeIter, indexTreeIter);
                }
            }
            else
            {
                // Normal case: compare HEAD with index
                diffs = git.diff().setCached(true).setShowNameAndStatusOnly(false).call();
            }

            // Format all found differences
            for (var diff : diffs)
            {
                diffFormatter.format(diff);
            }
        }
    }

    @Override
    public List<GitCommitInfo> getCommitHistory(Repository repository, int maxCommits)
        throws GitAPIException, IOException
    {
        var commits = new ArrayList<GitCommitInfo>();
        
        try (Git git = new Git(repository); RevWalk walk = new RevWalk(repository))
        {
            // Get current branch reference
            var headRef = repository.findRef(Constants.HEAD);
            if (headRef == null || headRef.getObjectId() == null)
            {
                return commits; // Empty repository
            }

            ObjectId headId = headRef.getObjectId();
            RevCommit headCommit = walk.parseCommit(headId);
            walk.markStart(headCommit);

            int count = 0;
            for (RevCommit commit : walk)
            {
                if (count >= maxCommits)
                {
                    break;
                }

                // Get changed files for this commit
                var changedFiles = getChangedFiles(repository, commit);
                
                var commitInfo = new GitCommitInfo(
                    commit.getName(),
                    commit.abbreviate(8).name(),
                    commit.getAuthorIdent().getName(),
                    commit.getAuthorIdent().getEmailAddress(),
                    commit.getCommitTime() * 1000L, // Convert seconds to milliseconds
                    commit.getFullMessage().trim(),
                    changedFiles
                );
                
                commits.add(commitInfo);
                count++;
            }
        }
        
        return commits;
    }

    private List<String> getChangedFiles(Repository repository, RevCommit commit)
        throws IOException
    {
        var changedFiles = new ArrayList<String>();
        
        try (Git git = new Git(repository))
        {
            // Get the parent commit (if exists)
            RevCommit[] parents = commit.getParents();
            if (parents.length == 0)
            {
                // Initial commit - get all files
                try (var walk = new org.eclipse.jgit.treewalk.TreeWalk(repository))
                {
                    walk.addTree(commit.getTree());
                    walk.setRecursive(true);
                    
                    while (walk.next())
                    {
                        changedFiles.add(walk.getPathString());
                    }
                }
            }
            else
            {
                // Compare with parent commit
                RevCommit parent = parents[0];
                try (DiffFormatter diffFormatter = new DiffFormatter(null))
                {
                    diffFormatter.setRepository(repository);
                    List<DiffEntry> diffs = diffFormatter.scan(parent.getTree(), commit.getTree());
                    
                    for (DiffEntry diff : diffs)
                    {
                        changedFiles.add(diff.getNewPath());
                    }
                }
            }
        }
        
        return changedFiles;
    }

    @Override
    public String getDiffText(Repository repository, int contextLines)
        throws GitAPIException, IOException
    {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream())
        {
            getDiff(repository, contextLines, outputStream);
            return outputStream.toString(StandardCharsets.UTF_8.name());
        }
    }

    @Override
    public String getUncommittedDiffText(Repository repository, int contextLines)
        throws GitAPIException, IOException
    {
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DiffFormatter diffFormatter = new DiffFormatter(outputStream))
        {
            diffFormatter.setRepository(repository);
            diffFormatter.setContext(contextLines);

            var headCommitId = repository.resolve(Constants.HEAD);
            var workingTreeIter = new FileTreeIterator(repository);
            if (headCommitId == null)
            {
                var diffs = diffFormatter.scan(new EmptyTreeIterator(), workingTreeIter);
                for (var diff : diffs)
                {
                    diffFormatter.format(diff);
                }
                return outputStream.toString(StandardCharsets.UTF_8.name());
            }

            var headCommit = repository.parseCommit(headCommitId);
            var headTreeIter = prepareTreeParser(repository, headCommit);
            var diffs = diffFormatter.scan(headTreeIter, workingTreeIter);
            for (var diff : diffs)
            {
                diffFormatter.format(diff);
            }

            return outputStream.toString(StandardCharsets.UTF_8.name());
        }
    }

    @Override
    public String getDiffText(Repository repository, String oldCommit, String newCommit, int contextLines)
        throws GitAPIException, IOException
    {
        try (Git git = new Git(repository); ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            DiffFormatter diffFormatter = new DiffFormatter(outputStream))
        {
            diffFormatter.setRepository(repository);
            diffFormatter.setContext(contextLines);

            // Resolve commit objects
            var oldRev = repository.parseCommit(repository.resolve(oldCommit));
            var newRev = repository.parseCommit(repository.resolve(newCommit));

            // Get diff between commits
            var diffs = git.diff()
                .setOldTree(prepareTreeParser(repository, oldRev))
                .setNewTree(prepareTreeParser(repository, newRev))
                .call();

            // Format all differences
            for (var diff : diffs)
            {
                diffFormatter.format(diff);
            }

            return outputStream.toString(StandardCharsets.UTF_8.name());
        }
    }

    private org.eclipse.jgit.treewalk.AbstractTreeIterator prepareTreeParser(Repository repository, RevCommit commit)
        throws IOException
    {
        try (var reader = repository.newObjectReader())
        {
            var tree = commit.getTree();
            var treeParser = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
            treeParser.reset(reader, tree);
            return treeParser;
        }
    }
}
