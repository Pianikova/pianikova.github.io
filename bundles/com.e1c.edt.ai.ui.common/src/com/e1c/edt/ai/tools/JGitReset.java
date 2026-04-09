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
            .addParameter("<paths>...", "Reset only specified paths");
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
            else if (!arg.startsWith("-"))
            {
                if (commit == null)
                {
                    commit = arg;
                }
                else
                {
                    paths.add(arg);
                }
            }
        }

        // Validate: --soft cannot be used with paths
        if (mode == ResetCommand.ResetType.SOFT && !paths.isEmpty())
        {
            return new GitCommandResult(1, "", "error: cannot do a soft reset with paths\n");
        }

        try
        {
            var resetCmd = git.reset();
            resetCmd.setMode(mode);
            if (commit != null)
            {
                resetCmd.setRef(commit);
            }
            for (var path : paths)
            {
                resetCmd.addPath(path);
            }
            resetCmd.call();

            return new GitCommandResult(0, "", "");
        }
        catch (Exception e)
        {
            var errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty())
            {
                errorMsg = "Failed to reset";
            }
            var stackTrace = e.getClass().getSimpleName();
            return new GitCommandResult(1, "", "error: " + errorMsg + " (" + stackTrace + ")\n");
        }
    }
}
