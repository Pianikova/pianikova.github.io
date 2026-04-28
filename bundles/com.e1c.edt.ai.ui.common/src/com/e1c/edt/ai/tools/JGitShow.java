/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.AnyObjectId;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

/**
 * Git show command implementation
 */
public class JGitShow implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "show"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Show various types of objects")
            .addParameter("(no arguments)", "Show the current commit")
            .addParameter("<object>", "Show a commit, tag, or tree object")
            .addParameter("<object>:<path>", "Show contents of a file at a specific commit")
            .addParameter("--stat", "Show statistics of changes instead of full diff");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args)
        throws IOException, GitAPIException, AmbiguousObjectException, IncorrectObjectTypeException
    {
        var repository = git.getRepository();

        if (args.isEmpty())
        {
            var headCommit = repository.parseCommit(repository.resolve(Constants.HEAD));
            return formatCommitDetails(git, headCommit, false);
        }

        var showStat = false;
        var objectIndex = -1;
        var pathIndex = -1;

        for (var i = 0; i < args.size(); i++)
        {
            if ("--stat".equals(args.get(i)))
            {
                showStat = true;
            }
            else if (!args.get(i).startsWith("-") && objectIndex == -1)
            {
                objectIndex = i;
            }
            else if (!args.get(i).startsWith("-") && objectIndex >= 0)
            {
                pathIndex = i;
                break;
            }
        }

        if (objectIndex == -1)
        {
            var headCommit = repository.parseCommit(repository.resolve(Constants.HEAD));
            return formatCommitDetails(git, headCommit, showStat);
        }

        var objectArg = args.get(objectIndex);
        var path = pathIndex >= 0 ? args.get(pathIndex) : null;
        var colonIndex = objectArg.indexOf(':');
        if (path == null && colonIndex > 0)
        {
            path = objectArg.substring(colonIndex + 1);
            objectArg = objectArg.substring(0, colonIndex);
        }

        var objectId = repository.resolve(objectArg);
        if (objectId == null)
        {
            return new GitCommandResult(1, "", "fatal: bad revision '" + objectArg + "'");
        }

        if (path != null && !path.isEmpty())
        {
            return showFileContent(git, repository, objectId, path);
        }
        else
        {
        try
        {
            var commit = repository.parseCommit(objectId);
            return formatCommitDetails(git, commit, showStat);
        }
        catch (IncorrectObjectTypeException e)
        {
            return showBlobContent(repository, objectId);
        }
        catch (Exception e)
        {
            return new GitCommandResult(1, "", "fatal: " + e.getClass().getName() + ": " + (e.getMessage() != null ? e.getMessage() : "unknown error"));
        }
        }
    }

    @SuppressWarnings("nls")
    private GitCommandResult formatCommitDetails(Git git, RevCommit commit, boolean showStat) throws IOException
    {
        try
        {
            var sb = new StringBuilder();
            var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            sb.append("commit ").append(commit.getName()).append("\n");
            
            var authorIdent = commit.getAuthorIdent();
            if (authorIdent != null)
            {
                sb.append("Author: ")
                    .append(authorIdent.getName())
                    .append(" <")
                    .append(authorIdent.getEmailAddress())
                    .append(">\n");
            }
            
            sb.append("Date:   ")
                .append(Instant.ofEpochSecond(commit.getCommitTime()).atZone(ZoneId.systemDefault()).format(dateFormatter))
                .append("\n");
            
            var fullMessage = commit.getFullMessage();
            sb.append("\n    ").append((fullMessage != null ? fullMessage.trim() : "")).append("\n\n");

            try
            {
                var repository = git.getRepository();
                var outputStream = new ByteArrayOutputStream();
                try (var formatter = new DiffFormatter(outputStream))
                {
                    formatter.setRepository(repository);
                    formatter.setContext(3);

                    try (var objectReader = repository.newObjectReader())
                    {
                        var newTree = commit.getTree();
                        if (newTree == null)
                        {
                            throw new Exception("Tree is null");
                        }

                        org.eclipse.jgit.treewalk.AbstractTreeIterator oldTreeIter;
                        if (commit.getParentCount() > 0)
                        {
                            var oldTree = commit.getParent(0).getTree();
                            var parser = new CanonicalTreeParser();
                            parser.reset(objectReader, oldTree.getId());
                            oldTreeIter = parser;
                        }
                        else
                        {
                            oldTreeIter = new EmptyTreeIterator();
                        }

                        var newTreeIter = new CanonicalTreeParser();
                        newTreeIter.reset(objectReader, newTree.getId());

                        List<org.eclipse.jgit.diff.DiffEntry> diffs = formatter.scan(oldTreeIter, newTreeIter);

                        if (diffs != null && !diffs.isEmpty())
                        {
                            if (showStat)
                            {
                                appendCommitStats(git, commit, diffs, sb);
                            }
                            else
                            {
                                for (var diff : diffs)
                                {
                                    formatter.format(diff);
                                }
                                sb.append(outputStream.toString(StandardCharsets.UTF_8.name()));
                            }
                        }
                        else
                        {
                            sb.append("(no changes)\n");
                        }
                    }
                }
            }
            catch (Exception diffException)
            {
                sb.append("(diff not available - error: ").append(diffException.getClass().getSimpleName()).append(": ")
                    .append(diffException.getMessage()).append(")\n");
            }

            return new GitCommandResult(0, sb.toString(), "");
        }
        catch (Exception e)
        {
            var stackTrace = "";
            for (var element : e.getStackTrace())
            {
                stackTrace += element.toString() + "\n";
                if (stackTrace.length() > 500)
                {
                    stackTrace += "...";
                    break;
                }
            }
            return new GitCommandResult(1, "", "fatal: " + e.getClass().getSimpleName() + ": " + (e.getMessage() != null ? e.getMessage() : "no message") + "\nStack trace:\n" + stackTrace);
        }
    }

    @SuppressWarnings("nls")
    private void appendCommitStats(Git git, RevCommit commit, List<org.eclipse.jgit.diff.DiffEntry> diffs, StringBuilder sb)
        throws IOException
    {
        var totalFiles = diffs.size();
        var totalInsertions = 0;
        var totalDeletions = 0;

        var outputStream = new ByteArrayOutputStream();
        try (var formatter = new DiffFormatter(outputStream))
        {
            formatter.setRepository(git.getRepository());

            for (var diff : diffs)
            {
                var insertions = 0;
                var deletions = 0;

                try
                {
                    formatter.format(diff);
                    var diffText = outputStream.toString(StandardCharsets.UTF_8.name());
                    outputStream.reset();

                    for (var line : diffText.split("\n"))
                    {
                        if (line.startsWith("+") && !line.startsWith("+++"))
                        {
                            insertions++;
                        }
                        else if (line.startsWith("-") && !line.startsWith("---"))
                        {
                            deletions++;
                        }
                    }
                }
                catch (Exception e)
                {
                    // If diff cannot be calculated, just skip stats for this file
                }

                totalInsertions += insertions;
                totalDeletions += deletions;

                if (insertions > 0 || deletions > 0)
                {
                    sb.append(" ").append(diff.getNewPath()).append(" | ");
                    if (insertions > 0 && deletions > 0)
                    {
                        sb.append(insertions).append(" insertions(+), ").append(deletions).append(" deletions(-)\n");
                    }
                    else if (insertions > 0)
                    {
                        sb.append(insertions).append(" insertion(+)\n");
                    }
                    else if (deletions > 0)
                    {
                        sb.append(deletions).append(" deletion(-)\n");
                    }
                }
            }
        }

        sb.append(" ").append(totalFiles).append(" file");
        if (totalFiles != 1)
        {
            sb.append("s");
        }
        sb.append(" changed");
        if (totalInsertions > 0 || totalDeletions > 0)
        {
            sb.append(", ");
            if (totalInsertions > 0)
            {
                sb.append(totalInsertions).append(" insertion");
                if (totalInsertions != 1)
                {
                    sb.append("s");
                }
                sb.append("(+)");
            }
            if (totalInsertions > 0 && totalDeletions > 0)
            {
                sb.append(", ");
            }
            if (totalDeletions > 0)
            {
                sb.append(totalDeletions).append(" deletion");
                if (totalDeletions != 1)
                {
                    sb.append("s");
                }
                sb.append("(-)");
            }
        }
        sb.append("\n");
    }

    @SuppressWarnings("nls")
    private GitCommandResult showFileContent(Git git, Repository repository, AnyObjectId objectId, String path)
        throws IOException, IncorrectObjectTypeException
    {
        var commit = repository.parseCommit(objectId);
        var tree = commit.getTree();

        try (var treeWalk = new org.eclipse.jgit.treewalk.TreeWalk(repository))
        {
            treeWalk.addTree(tree);
            treeWalk.setRecursive(true);
            treeWalk.setFilter(org.eclipse.jgit.treewalk.filter.PathFilter.create(path));

            if (!treeWalk.next())
            {
                return new GitCommandResult(1, "", "fatal: '" + path + "' not found in commit");
            }

            var objectIdFile = treeWalk.getObjectId(0);
            var loader = repository.open(objectIdFile);

            if (loader.isLarge())
            {
                return new GitCommandResult(1, "", "fatal: file is too large to show");
            }

            var content = new String(loader.getBytes(), StandardCharsets.UTF_8);
            return new GitCommandResult(0, content, "");
        }
    }

    @SuppressWarnings("nls")
    private GitCommandResult showBlobContent(Repository repository, AnyObjectId objectId) throws IOException
    {
        var loader = repository.open(objectId);
        if (loader.isLarge())
        {
            return new GitCommandResult(1, "", "fatal: object is too large to show");
        }

        var content = new String(loader.getBytes(), StandardCharsets.UTF_8);
        return new GitCommandResult(0, content, "");
    }
}
