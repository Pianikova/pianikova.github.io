/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;

/**
 * Git rm command implementation
 */
public class JGitRm implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "rm"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Remove files from the working tree and from the index")
            .addParameter("--cached", "Unstage and remove paths only from the index")
            .addParameter("<pathspec>...", "Files to remove");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args)
    {
        var rmCmd = git.rm();
        var cached = false;
        var paths = new ArrayList<String>();

        for (var arg : args)
        {
            if (arg.equals("--cached"))
            {
                cached = true;
            }
            else if (!arg.startsWith("-"))
            {
                rmCmd.addFilepattern(arg);
                paths.add(arg);
            }
        }

        if (cached)
        {
            rmCmd.setCached(true);
        }

        if (paths.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: no pathspec was given. Which files should I remove?\n");
        }

        try
        {
            rmCmd.call();

            return new GitCommandResult(0, "", "");
        }
        catch (Exception e)
        {
            var errorMsg = e.getMessage();
            var cause = e.getCause();
            if (cause != null && cause.getMessage() != null)
            {
                errorMsg = cause.getMessage();
            }
            if (errorMsg == null || errorMsg.isEmpty())
            {
                errorMsg = "Failed to remove files";
            }
            return new GitCommandResult(1, "", "error: " + errorMsg + "\n");
        }
    }
}
