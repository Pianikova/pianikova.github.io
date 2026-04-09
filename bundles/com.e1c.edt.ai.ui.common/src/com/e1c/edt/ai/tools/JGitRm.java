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

        try
        {
            var result = rmCmd.call();
            if (paths.isEmpty())
            {
                return new GitCommandResult(0, "", "");
            }
            
            var anyRemoved = false;
            for (var path : paths)
            {
                if (result.findEntry(path) < 0)
                {
                    anyRemoved = true;
                    break;
                }
            }
            
            if (!anyRemoved)
            {
                return new GitCommandResult(1, "", "error: pathspec did not match any files\n");
            }
            return new GitCommandResult(0, "", "");
        }
        catch (Exception e)
        {
            var errorMsg = e.getMessage();
            if (errorMsg == null || errorMsg.isEmpty())
            {
                errorMsg = "Failed to remove files";
            }
            return new GitCommandResult(1, "", "error: " + errorMsg + "\n");
        }
    }
}
