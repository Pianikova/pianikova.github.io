/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.List;

import org.eclipse.jgit.api.Git;

/**
 * Git stash command implementation
 */
public class JGitStash implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "stash"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Stash the changes in a dirty working directory away")
            .addParameter("(no arguments)", "Stash current changes (alias for `push`)")
            .addParameter("push [-m <msg>] [-u]", "Stash current changes with optional message and untracked files")
            .addParameter("save [-m <msg>] [-u]", "Legacy alias for push")
            .addParameter("list", "List stashed changes")
            .addParameter("show [<stash>]", "Show files changed in the stash (default: stash@{0})")
            .addParameter("apply [<stash>]", "Apply stashed changes")
            .addParameter("pop [<stash>]", "Apply and remove stashed changes")
            .addParameter("drop [<stash>]", "Remove stashed changes")
            .addParameter("clear", "Remove all stash entries")
            .addParameter("-u, --include-untracked", "Include untracked files (push/save only)")
            .addParameter("-m <msg>", "Stash message (push/save only)");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args)
    {
        if (args.isEmpty())
        {
            return doPush(git, null, false);
        }

        var subCommand = args.get(0);
        switch (subCommand)
        {
            case "push":
            case "save":
                return doPush(git, parseMessage(args), parseIncludeUntracked(args));
            case "list":
                return doList(git);
            case "show":
                return doShow(git, args.size() > 1 ? args.get(1) : "stash@{0}");
            case "apply":
                return doApply(git, args.size() > 1 ? args.get(1) : "stash@{0}");
            case "drop":
                return doDrop(git, args.size() > 1 ? extractStashIndex(args.get(1)) : 0);
            case "pop":
                return doPop(git, args.size() > 1 ? extractStashIndex(args.get(1)) : 0);
            case "clear":
                return doClear(git);
            default:
                // Bare options without subcommand → treat as `push`
                if (subCommand.startsWith("-"))
                {
                    return doPush(git, parseMessage(args), parseIncludeUntracked(args));
                }
                return new GitCommandResult(1, "", "error: unknown stash command: " + subCommand + "\n");
        }
    }

    @SuppressWarnings("nls")
    private GitCommandResult doPush(Git git, String message, boolean includeUntracked)
    {
        try
        {
            var cmd = git.stashCreate().setIncludeUntracked(includeUntracked);
            if (message != null)
            {
                cmd.setWorkingDirectoryMessage(message);
                cmd.setIndexMessage(message);
            }
            var stashCommit = cmd.call();
            if (stashCommit == null)
            {
                return new GitCommandResult(0, "No local changes to save\n", "");
            }
            return new GitCommandResult(0,
                "Saved working directory and index state " + stashCommit.getName() + "\n", "");
        }
        catch (Exception e)
        {
            return wrapError(e, "Failed to stash");
        }
    }

    @SuppressWarnings("nls")
    private GitCommandResult doList(Git git)
    {
        try
        {
            var stashes = git.stashList().call();
            var sb = new StringBuilder();
            var index = 0;
            for (var stash : stashes)
            {
                sb.append("stash@{").append(index++).append("}: ")
                    .append(stash.getShortMessage()).append("\n");
            }
            return new GitCommandResult(0, sb.toString(), "");
        }
        catch (Exception e)
        {
            return wrapError(e, "Failed to list stashes");
        }
    }

    @SuppressWarnings("nls")
    private GitCommandResult doShow(Git git, String ref)
    {
        try
        {
            var idx = extractStashIndex(ref);
            int i = 0;
            org.eclipse.jgit.revwalk.RevCommit target = null;
            for (var s : git.stashList().call())
            {
                if (i++ == idx)
                {
                    target = s;
                    break;
                }
            }
            if (target == null)
            {
                return new GitCommandResult(1, "", "error: " + ref + " is not a valid reference\n");
            }
            var parent = target.getParent(0);
            var sb = new StringBuilder();
            try (var reader = git.getRepository().newObjectReader();
                var walk = new org.eclipse.jgit.revwalk.RevWalk(git.getRepository()))
            {
                var oldTreeIter = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
                oldTreeIter.reset(reader, walk.parseCommit(parent.getId()).getTree());
                var newTreeIter = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
                newTreeIter.reset(reader, target.getTree());
                var diffs = git.diff().setOldTree(oldTreeIter).setNewTree(newTreeIter).call();
                for (var d : diffs)
                {
                    sb.append(d.getChangeType()).append("\t");
                    sb.append(d.getNewPath().equals("/dev/null") ? d.getOldPath() : d.getNewPath()).append("\n");
                }
            }
            return new GitCommandResult(0, sb.toString(), "");
        }
        catch (Exception e)
        {
            return wrapError(e, "Failed to show stash");
        }
    }

    @SuppressWarnings("nls")
    private GitCommandResult doApply(Git git, String ref)
    {
        try
        {
            git.stashApply().setStashRef("stash@{" + extractStashIndex(ref) + "}").call();
            return new GitCommandResult(0, "", "");
        }
        catch (Exception e)
        {
            return wrapError(e, "Failed to apply stash");
        }
    }

    @SuppressWarnings("nls")
    private GitCommandResult doDrop(Git git, int idx)
    {
        try
        {
            git.stashDrop().setStashRef(idx).call();
            return new GitCommandResult(0, "", "");
        }
        catch (Exception e)
        {
            return wrapError(e, "Failed to drop stash");
        }
    }

    @SuppressWarnings("nls")
    private GitCommandResult doPop(Git git, int idx)
    {
        try
        {
            git.stashApply().setStashRef("stash@{" + idx + "}").call();
            git.stashDrop().setStashRef(idx).call();
            return new GitCommandResult(0, "", "");
        }
        catch (Exception e)
        {
            return wrapError(e, "Failed to pop stash");
        }
    }

    @SuppressWarnings("nls")
    private GitCommandResult doClear(Git git)
    {
        try
        {
            git.stashDrop().setAll(true).call();
            return new GitCommandResult(0, "", "");
        }
        catch (Exception e)
        {
            return wrapError(e, "Failed to clear stash");
        }
    }

    @SuppressWarnings("nls")
    private static String parseMessage(List<String> args)
    {
        for (int i = 1; i < args.size(); i++)
        {
            if (args.get(i).equals("-m") && i + 1 < args.size())
            {
                return args.get(i + 1);
            }
        }
        return null;
    }

    @SuppressWarnings("nls")
    private static boolean parseIncludeUntracked(List<String> args)
    {
        for (var arg : args)
        {
            if (arg.equals("-u") || arg.equals("--include-untracked"))
            {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("nls")
    private static GitCommandResult wrapError(Exception e, String fallback)
    {
        var msg = e.getMessage();
        if (msg == null || msg.isEmpty())
        {
            msg = fallback;
        }
        return new GitCommandResult(1, "", "error: " + msg + "\n");
    }

    @SuppressWarnings("nls")
    private static int extractStashIndex(String stashRef)
    {
        if (stashRef == null)
        {
            return 0;
        }
        if (stashRef.startsWith("stash@{"))
        {
            var end = stashRef.indexOf('}');
            if (end > 0)
            {
                try
                {
                    return Integer.parseInt(stashRef.substring("stash@{".length(), end));
                }
                catch (NumberFormatException e)
                {
                    // ignore
                }
            }
        }
        try
        {
            return Integer.parseInt(stashRef);
        }
        catch (NumberFormatException e)
        {
            return 0;
        }
    }
}
