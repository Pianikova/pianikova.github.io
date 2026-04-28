/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectReader;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.treewalk.AbstractTreeIterator;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;
import org.eclipse.jgit.treewalk.filter.PathFilterGroup;

import com.google.inject.Inject;

/**
 * Git diff command implementation
 */
public class JGitDiff implements IJGitCommand
{
    private final IJGitCommonHelper commonHelper;

    @Inject
    public JGitDiff(IJGitCommonHelper commonHelper)
    {
        this.commonHelper = commonHelper;
    }

    @Override
    public String getName()
    {
        return "diff"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Show changes between commits, commit and working tree, etc")
            .addParameter("-U<n>", "Generate diffs with <n> lines of context (default: 3)")
            .addParameter("--cached", "View changes you staged for commit")
            .addParameter("--staged", "Same as --cached")
            .addParameter("--stat", "Show a diffstat instead of the patch")
            .addParameter("--name-only", "Show only names of changed files")
            .addParameter("--name-status", "Show names and statuses (A/M/D/R) of changed files")
            .addParameter("<commit> <commit>", "Show changes between two commits")
            .addParameter("<path>...", "Limit to specified paths");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws IOException, GitAPIException
    {
        var contextLines = 3;
        var cached = false;
        var stat = false;
        var nameOnly = false;
        var nameStatus = false;
        var revisions = new ArrayList<String>();
        var paths = new ArrayList<String>();
        var pathsOnly = false;

        for (var arg : args)
        {
            if (arg.equals("--"))
            {
                pathsOnly = true;
            }
            else if (pathsOnly)
            {
                paths.add(arg);
            }
            else if (!pathsOnly && (arg.startsWith("-U") || arg.startsWith("--unified=")))
            {
                var numStr = arg.startsWith("-U") ? arg.substring(2) : arg.substring("--unified=".length());
                try
                {
                    contextLines = Integer.parseInt(numStr);
                }
                catch (NumberFormatException e)
                {
                    // ignore
                }
            }
            else if (!pathsOnly && (arg.equals("--cached") || arg.equals("--staged")))
            {
                cached = true;
            }
            else if (!pathsOnly && arg.equals("--stat"))
            {
                stat = true;
            }
            else if (!pathsOnly && arg.equals("--name-only"))
            {
                nameOnly = true;
            }
            else if (!pathsOnly && arg.equals("--name-status"))
            {
                nameStatus = true;
            }
            else if (!arg.startsWith("-"))
            {
                revisions.add(arg);
            }
        }

        var repository = git.getRepository();
        var parsedArgs = splitRevisionsAndPaths(repository, revisions);
        revisions = parsedArgs.revisions;
        paths.addAll(parsedArgs.paths);

        var outputStream = new ByteArrayOutputStream();
        try (DiffFormatter formatter = new DiffFormatter(outputStream);
            var reader = repository.newObjectReader())
        {
            formatter.setRepository(repository);
            formatter.setContext(contextLines);

            List<DiffEntry> diffs = computeDiff(repository, reader, formatter, cached, revisions);
            if (!cached && revisions.size() < 2)
            {
                diffs = filterUntracked(git, diffs);
            }

            var filtered = new ArrayList<DiffEntry>();
            for (var diff : diffs)
            {
                if (paths.isEmpty() || paths.stream().anyMatch(p -> diff.getNewPath().contains(p)
                    || diff.getOldPath().contains(p)))
                {
                    filtered.add(diff);
                }
            }

            if (!nameOnly && !nameStatus && !stat)
            {
                if (cached)
                {
                    formatter.format(filtered);
                    return new GitCommandResult(0, outputStream.toString(StandardCharsets.UTF_8), "");
                }
                if (revisions.size() < 2)
                {
                    return renderPatchEntries(git, revisions, filtered, contextLines);
                }
                return renderPatch(git, cached, revisions, paths, contextLines);
            }
            if (nameOnly)
            {
                var sb = new StringBuilder();
                for (var d : filtered)
                {
                    sb.append(pathFor(d)).append("\n");
                }
                return new GitCommandResult(0, sb.toString(), "");
            }
            if (nameStatus)
            {
                var sb = new StringBuilder();
                for (var d : filtered)
                {
                    sb.append(statusChar(d)).append("\t").append(pathFor(d)).append("\n");
                }
                return new GitCommandResult(0, sb.toString(), "");
            }
            if (stat)
            {
                var sb = new StringBuilder();
                var totalAdd = 0;
                var totalDel = 0;
                for (var d : filtered)
                {
                    var diffText = renderPatch(git, cached, revisions, List.of(pathFor(d)), 0).stdOut;
                    int add = 0;
                    int del = 0;
                    for (var line : diffText.split("\n"))
                    {
                        if (line.startsWith("+") && !line.startsWith("+++"))
                        {
                            add++;
                        }
                        else if (line.startsWith("-") && !line.startsWith("---"))
                        {
                            del++;
                        }
                    }
                    totalAdd += add;
                    totalDel += del;
                    sb.append(" ").append(pathFor(d)).append(" | ").append(add + del)
                        .append(" ").append("+".repeat(Math.min(add, 40)))
                        .append("-".repeat(Math.min(del, 40))).append("\n");
                }
                sb.append(" ").append(filtered.size()).append(" files changed, ")
                    .append(totalAdd).append(" insertions(+), ")
                    .append(totalDel).append(" deletions(-)\n");
                return new GitCommandResult(0, sb.toString(), "");
            }

            for (var d : filtered)
            {
                formatter.format(d);
            }
        }

        return new GitCommandResult(0, outputStream.toString(StandardCharsets.UTF_8), "");
    }

    private static GitCommandResult renderPatchEntries(Git git, List<String> revisions, List<DiffEntry> diffs,
        int contextLines) throws IOException, GitAPIException
    {
        var sb = new StringBuilder();
        for (var diff : diffs)
        {
            sb.append(renderPatch(git, false, revisions, List.of(pathFor(diff)), contextLines).stdOut);
        }
        return new GitCommandResult(0, sb.toString(), "");
    }

    private static GitCommandResult renderPatch(Git git, boolean cached, List<String> revisions, List<String> paths,
        int contextLines) throws IOException, GitAPIException
    {
        var repository = git.getRepository();
        var outputStream = new ByteArrayOutputStream();
        var diffCommand = git.diff()
            .setOutputStream(outputStream)
            .setContextLines(contextLines);

        if (!paths.isEmpty())
        {
            diffCommand.setPathFilter(PathFilterGroup.createFromStrings(paths));
        }

        if (cached && revisions.isEmpty() && repository.resolve(Constants.HEAD) == null)
        {
            try (var formatter = new DiffFormatter(outputStream))
            {
                formatter.setRepository(repository);
                formatter.setContext(contextLines);
                if (!paths.isEmpty())
                {
                    formatter.setPathFilter(PathFilterGroup.createFromStrings(paths));
                }
                formatter.format(new EmptyTreeIterator(), new DirCacheIterator(repository.readDirCache()));
            }
        }
        else if (revisions.size() >= 2)
        {
            try (var reader = repository.newObjectReader())
            {
                diffCommand.setOldTree(treeIterator(repository, reader, revisions.get(0)));
                diffCommand.setNewTree(treeIterator(repository, reader, revisions.get(1)));
                diffCommand.call();
            }
        }
        else if (revisions.size() == 1)
        {
            try (var reader = repository.newObjectReader())
            {
                diffCommand.setOldTree(treeIterator(repository, reader, revisions.get(0)));
                diffCommand.setCached(cached);
                diffCommand.call();
            }
        }
        else
        {
            diffCommand.setCached(cached);
            diffCommand.call();
        }

        return new GitCommandResult(0, outputStream.toString(StandardCharsets.UTF_8), "");
    }

    private static List<DiffEntry> filterUntracked(Git git, List<DiffEntry> diffs) throws GitAPIException
    {
        var status = git.status().call();
        var untracked = status.getUntracked();
        var untrackedFolders = status.getUntrackedFolders();

        var result = new ArrayList<DiffEntry>();
        for (var diff : diffs)
        {
            var path = pathFor(diff);
            if (diff.getChangeType() == DiffEntry.ChangeType.ADD
                && (untracked.contains(path) || matchesAnyFolder(path, untrackedFolders)))
            {
                continue;
            }
            result.add(diff);
        }
        return result;
    }

    private static boolean matchesAnyFolder(String path, Set<String> folders)
    {
        for (var folder : folders)
        {
            var prefix = folder.endsWith("/") ? folder : folder + "/";
            if (path.startsWith(prefix))
            {
                return true;
            }
        }
        return false;
    }

    private List<DiffEntry> computeDiff(Repository repository, ObjectReader reader, DiffFormatter formatter,
        boolean cached, List<String> revisions)
        throws IOException
    {
        if (revisions.size() >= 2)
        {
            return formatter.scan(treeIterator(repository, reader, revisions.get(0)),
                treeIterator(repository, reader, revisions.get(1)));
        }
        if (revisions.size() == 1)
        {
            var oldTree = treeIterator(repository, reader, revisions.get(0));
            if (cached)
            {
                return formatter.scan(oldTree, new DirCacheIterator(repository.readDirCache()));
            }
            return formatter.scan(oldTree, new FileTreeIterator(repository));
        }

        if (cached)
        {
            var headObjectId = repository.resolve(Constants.HEAD);
            if (headObjectId == null)
            {
                return formatter.scan(new EmptyTreeIterator(), new DirCacheIterator(repository.readDirCache()));
            }
            var headTreeIter = treeIterator(repository, reader, Constants.HEAD);
            return formatter.scan(headTreeIter, new DirCacheIterator(repository.readDirCache()));
        }
        var dirCacheIter = new DirCacheIterator(repository.readDirCache());
        var workingTreeIter = new FileTreeIterator(repository);
        return formatter.scan(dirCacheIter, workingTreeIter);
    }

    private static ParsedArgs splitRevisionsAndPaths(Repository repository, List<String> args) throws IOException
    {
        var revisions = new ArrayList<String>();
        var paths = new ArrayList<String>();

        for (var arg : args)
        {
            if (paths.isEmpty() && revisions.size() < 2 && isRevision(repository, arg))
            {
                revisions.add(arg);
            }
            else
            {
                paths.add(arg);
            }
        }

        return new ParsedArgs(revisions, paths);
    }

    @SuppressWarnings("nls")
    private static boolean isRevision(Repository repository, String arg) throws IOException
    {
        return repository.resolve(arg + "^{tree}") != null || repository.resolve(arg) != null;
    }

    @SuppressWarnings("nls")
    private static AbstractTreeIterator treeIterator(Repository repository, ObjectReader reader, String revision)
        throws IOException
    {
        var treeId = repository.resolve(revision + "^{tree}");
        if (treeId == null)
        {
            var objectId = repository.resolve(revision);
            if (objectId == null)
            {
                return new EmptyTreeIterator();
            }
            treeId = repository.parseCommit(objectId).getTree().getId();
        }

        var treeParser = new CanonicalTreeParser();
        treeParser.reset(reader, treeId);
        return treeParser;
    }

    @SuppressWarnings("nls")
    private static String pathFor(DiffEntry d)
    {
        return "/dev/null".equals(d.getNewPath()) ? d.getOldPath() : d.getNewPath();
    }

    @SuppressWarnings("nls")
    private static String statusChar(DiffEntry d)
    {
        switch (d.getChangeType())
        {
            case ADD: return "A";
            case DELETE: return "D";
            case MODIFY: return "M";
            case RENAME: return "R";
            case COPY: return "C";
            default: return "?";
        }
    }

    private static final class ParsedArgs
    {
        final ArrayList<String> revisions;
        final ArrayList<String> paths;

        ParsedArgs(ArrayList<String> revisions, ArrayList<String> paths)
        {
            this.revisions = revisions;
            this.paths = paths;
        }
    }
}
