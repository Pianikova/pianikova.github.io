/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ResetCommand;

/**
 * Git reset command implementation
 */
public class JGitReset implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "reset"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Reset current HEAD to the specified state")
            .addParameter("--soft", "Reset but keep changes staged")
            .addParameter("--mixed", "Reset index but keep working directory changes (default)")
            .addParameter("--hard", "Reset index and working directory (discard all changes)")
            .addParameter("<commit>", "Commit to reset to (default: HEAD)")
            .addParameter("<paths>...", "Reset only specified paths (unstage files); mode flags are ignored when paths are given");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args)
    {
        ResetCommand.ResetType mode = ResetCommand.ResetType.MIXED;
        String commit = null;
        var paths = new ArrayList<String>();
        boolean modeSpecified = false;

        for (var arg : args)
        {
            if (arg.equals("--soft"))
            {
                if (modeSpecified)
                {
                    return new GitCommandResult(1, "", "error: cannot specify multiple reset modes\n");
                }
                mode = ResetCommand.ResetType.SOFT;
                modeSpecified = true;
            }
            else if (arg.equals("--hard"))
            {
                if (modeSpecified)
                {
                    return new GitCommandResult(1, "", "error: cannot specify multiple reset modes\n");
                }
                mode = ResetCommand.ResetType.HARD;
                modeSpecified = true;
            }
            else if (arg.equals("--mixed"))
            {
                if (modeSpecified)
                {
                    return new GitCommandResult(1, "", "error: cannot specify multiple reset modes\n");
                }
                mode = ResetCommand.ResetType.MIXED;
                modeSpecified = true;
            }
            else if (arg.equals("--"))
            {
                // explicit end-of-options separator; remaining args are paths
                // handled by falling through to next args, but since we process in one pass,
                // subsequent non-flag args will be treated as paths (commit already consumed)
            }
            else if (!arg.startsWith("-"))
            {
                if (commit == null && paths.isEmpty())
                {
                    commit = arg;
                }
                else
                {
                    paths.add(arg);
                }
            }
        }

        // When paths are specified, perform a per-path index reset (unstage).
        // Mode flags are not applicable for path-based reset — they are silently ignored,
        // matching real git behavior: "git reset HEAD -- file.txt" unstages file.txt.
        if (!paths.isEmpty())
        {
            if (mode == ResetCommand.ResetType.SOFT)
            {
                return new GitCommandResult(1, "", "error: cannot do a soft reset with paths\n");
            }
            return resetPaths(git, commit != null ? commit : "HEAD", paths);
        }

        // Full HEAD reset
        try
        {
            var resetCmd = git.reset();
            resetCmd.setMode(mode);
            if (commit != null)
            {
                resetCmd.setRef(commit);
            }
            resetCmd.call();

            var modeLabel = mode == ResetCommand.ResetType.SOFT ? "soft"
                : mode == ResetCommand.ResetType.HARD ? "hard" : "mixed";
            var ref = commit != null ? commit : "HEAD";
            return new GitCommandResult(0, "HEAD is now at " + ref + " (--" + modeLabel + ")\n", "");
        }
        catch (Exception e)
        {
            var errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty())
            {
                errorMsg = "Failed to reset";
            }
            return new GitCommandResult(1, "", "error: " + errorMsg + " (" + e.getClass().getSimpleName() + ")\n");
        }
    }

    /**
     * Resets specific paths in the index to match the given ref (unstage).
     * Equivalent to: git reset <ref> -- path1 path2 ...
     */
    @SuppressWarnings("nls")
    private static GitCommandResult resetPaths(Git git, String ref, List<String> paths)
    {
        try
        {
            var resetCmd = git.reset();
            resetCmd.setRef(ref);
            for (var p : paths)
            {
                resetCmd.addPath(p.replace('\\', '/'));
            }
            resetCmd.call();

            var sb = new StringBuilder();
            for (var p : paths)
            {
                sb.append("Unstaged changes after reset:\nM\t").append(p).append("\n");
            }
            return new GitCommandResult(0, sb.toString(), "");
        }
        catch (Exception e)
        {
            var errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty())
            {
                errorMsg = "Failed to reset paths";
            }
            return new GitCommandResult(1, "", "error: " + errorMsg + " (" + e.getClass().getSimpleName() + ")\n");
        }
    }
}
