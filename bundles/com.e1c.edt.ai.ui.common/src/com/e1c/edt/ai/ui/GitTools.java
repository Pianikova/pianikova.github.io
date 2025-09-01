/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

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
}
