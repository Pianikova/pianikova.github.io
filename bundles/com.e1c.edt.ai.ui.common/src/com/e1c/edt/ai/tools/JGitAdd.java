/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git add command implementation
 */
public class JGitAdd implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "add"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Add file contents to the index")
            .addParameter("<pathspec>...", "Files to add to the staging area")
            .addParameter(".", "Add all changes in the working tree (new, modified, deleted)")
            .addParameter("-A, --all", "Stage all changes (new, modified, deleted) - same as '.'")
            .addParameter("-u, --update", "Stage modifications and deletions only (no new files)");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException
    {
        var all = false;
        var update = false;
        var paths = new java.util.ArrayList<String>();

        for (var arg : args)
        {
            if (arg.equals("-A") || arg.equals("--all"))
            {
                all = true;
            }
            else if (arg.equals("-u") || arg.equals("--update"))
            {
                update = true;
            }
            else if (!arg.startsWith("-"))
            {
                paths.add(arg);
            }
        }

        // "add ." is treated the same as "-A": stage all changes including deletions
        var addDot = paths.size() == 1 && paths.get(0).equals(".");

        if (all || addDot || (paths.isEmpty() && !update))
        {
            return stageAll(git);
        }
        else if (update)
        {
            return stageUpdate(git, paths);
        }
        else
        {
            return stagePaths(git, paths);
        }
    }

    /**
     * Stage all changes: new + modified + deleted.
     * Uses status-based enumeration so untracked files, directories and deletions
     * are all handled consistently across JGit versions.
     */
    private static GitCommandResult stageAll(Git git) throws GitAPIException
    {
        var status = git.status().call();

        Set<String> toAdd = new LinkedHashSet<>();
        toAdd.addAll(status.getUntracked());
        toAdd.addAll(status.getUntrackedFolders());
        toAdd.addAll(status.getModified());
        toAdd.addAll(status.getConflicting());

        var missing = new LinkedHashSet<>(status.getMissing());

        if (!toAdd.isEmpty())
        {
            stageAdditions(git, toAdd);
        }

        stageRemovals(git, missing);

        var statusAfter = git.status().call();
        Set<String> actuallyStaged = new LinkedHashSet<>();
        Set<String> actuallyRemoved = new LinkedHashSet<>();

        actuallyStaged.addAll(statusAfter.getAdded());
        actuallyStaged.addAll(statusAfter.getChanged());
        actuallyRemoved.addAll(statusAfter.getRemoved());

        return buildResult(actuallyStaged, actuallyRemoved);
    }

    /**
     * Stage modifications and deletions for tracked files (-u/--update).
     * New untracked files are ignored.
     */
    @SuppressWarnings("nls")
    private static GitCommandResult stageUpdate(Git git, List<String> paths) throws GitAPIException
    {
        var status = git.status().call();

        Set<String> modified = new LinkedHashSet<>(status.getModified());
        Set<String> missing = new LinkedHashSet<>(status.getMissing());

        // If specific paths given, filter to those
        if (!paths.isEmpty())
        {
            modified.removeIf(path -> !matchesAnyPathspec(path, paths));
            missing.removeIf(path -> !matchesAnyPathspec(path, paths));
        }

        if (!modified.isEmpty() || !missing.isEmpty())
        {
            var addCmd = git.add().setUpdate(true);
            if (!paths.isEmpty())
            {
                for (var p : paths)
                {
                    addCmd.addFilepattern(normalizePath(p));
                }
            }
            else
            {
                addCmd.addFilepattern(".");
            }
            addCmd.call();
        }

        stageRemovals(git, missing);

        var statusAfter = git.status().call();
        Set<String> actuallyStaged = new LinkedHashSet<>();
        Set<String> actuallyRemoved = new LinkedHashSet<>();

        actuallyStaged.addAll(filterByPathspec(statusAfter.getChanged(), paths));
        actuallyRemoved.addAll(filterByPathspec(statusAfter.getRemoved(), paths));

        return buildResult(actuallyStaged, actuallyRemoved);
    }

    /**
     * Stage specific paths by name.
     * Resolves the correct JGit call depending on whether the path is
     * new/modified (add) or missing (rm).
     */
    @SuppressWarnings("nls")
    private static GitCommandResult stagePaths(Git git, List<String> paths) throws GitAPIException
    {
        if (paths.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: no paths specified");
        }

        var status = git.status().call();
        Set<String> modifiedAll = new LinkedHashSet<>(status.getModified());
        Set<String> missingAll = new LinkedHashSet<>(status.getMissing());
        Set<String> untrackedAll = new LinkedHashSet<>(status.getUntracked());
        Set<String> untrackedFoldersAll = new LinkedHashSet<>(status.getUntrackedFolders());
        var workTree = git.getRepository().getWorkTree();

        Set<String> toAdd = new LinkedHashSet<>();
        Set<String> toRemove = new LinkedHashSet<>();
        Set<String> notFound = new LinkedHashSet<>();

        for (var p : paths)
        {
            var normalized = normalizePath(p);
            if (missingAll.contains(normalized))
            {
                toRemove.add(normalized);
            }
            else if (containsPathspecMatch(modifiedAll, normalized) || containsPathspecMatch(untrackedAll, normalized)
                || containsPathspecMatch(untrackedFoldersAll, normalized))
            {
                toAdd.add(normalized);
            }
            else
            {
                var file = new File(workTree, normalized.replace('/', File.separatorChar));
                if (file.exists())
                {
                    toAdd.add(normalized);
                }
                else
                {
                    notFound.add(normalized);
                }
            }
        }

        if (!toAdd.isEmpty())
        {
            stageAdditions(git, toAdd);
        }

        stageRemovals(git, toRemove);

        if (!notFound.isEmpty())
        {
            var sb = new StringBuilder();
            for (var f : notFound)
            {
                sb.append("fatal: pathspec '").append(f).append("' did not match any files\n");
            }
            return new GitCommandResult(1, "", sb.toString());
        }

        var statusAfter = git.status().call();
        Set<String> actuallyStaged = new LinkedHashSet<>();
        Set<String> actuallyRemoved = new LinkedHashSet<>();

        for (var p : paths)
        {
            var normalized = normalizePath(p);
            if (statusAfter.getAdded().contains(normalized))
            {
                actuallyStaged.add(normalized);
            }
            else if (statusAfter.getRemoved().contains(normalized))
            {
                actuallyRemoved.add(normalized);
            }
            else if (statusAfter.getChanged().contains(normalized))
            {
                actuallyStaged.add(normalized);
            }
        }

        return buildResult(actuallyStaged, actuallyRemoved);
    }

    private static void stageAdditions(Git git, Set<String> paths) throws GitAPIException
    {
        var addCmd = git.add();
        for (var f : paths)
        {
            addCmd.addFilepattern(f);
        }
        addCmd.call();
    }

    /** Stages deletions by removing files from the index (--cached). */
    private static void stageRemovals(Git git, Set<String> missing) throws GitAPIException
    {
        if (missing.isEmpty())
        {
            return;
        }
        var rmCmd = git.rm().setCached(true);
        for (var f : missing)
        {
            rmCmd.addFilepattern(f);
        }
        try
        {
            rmCmd.call();
        }
        catch (Exception ignore)
        {
            // best-effort: don't fail the whole add if rm has issues
        }
    }

    /** Builds an informative result message listing what was staged and removed. */
    @SuppressWarnings("nls")
    private static GitCommandResult buildResult(Set<String> staged, Set<String> removed)
    {
        var sb = new StringBuilder();
        if (!staged.isEmpty())
        {
            for (var f : staged)
            {
                sb.append("add '").append(f).append("'\n");
            }
        }
        if (!removed.isEmpty())
        {
            for (var f : removed)
            {
                sb.append("remove '").append(f).append("'\n");
            }
        }
        if (sb.length() == 0)
        {
            sb.append("nothing to stage\n");
        }
        return new GitCommandResult(0, sb.toString(), "");
    }

    /** Normalizes a path to forward slashes (JGit convention). */
    private static String normalizePath(String p)
    {
        return p.replace('\\', '/');
    }

    private static boolean containsPathspecMatch(Set<String> files, String pathspec)
    {
        for (var file : files)
        {
            if (matchesPathspec(file, pathspec))
            {
                return true;
            }
        }
        return false;
    }

    private static Set<String> filterByPathspec(Set<String> files, List<String> pathspecs)
    {
        Set<String> result = new LinkedHashSet<>();
        for (var file : files)
        {
            if (matchesAnyPathspec(file, pathspecs))
            {
                result.add(file);
            }
        }
        return result;
    }

    private static boolean matchesAnyPathspec(String file, List<String> pathspecs)
    {
        if (pathspecs.isEmpty())
        {
            return true;
        }
        for (var pathspec : pathspecs)
        {
            if (matchesPathspec(file, normalizePath(pathspec)))
            {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesPathspec(String file, String pathspec)
    {
        if (pathspec.equals(".")) //$NON-NLS-1$
        {
            return true;
        }
        if (file.equals(pathspec))
        {
            return true;
        }
        var prefix = pathspec.endsWith("/") ? pathspec : pathspec + "/"; //$NON-NLS-1$ //$NON-NLS-2$
        return file.startsWith(prefix);
    }
}
