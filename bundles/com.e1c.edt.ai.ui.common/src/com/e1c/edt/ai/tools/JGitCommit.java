/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git commit command implementation
 */
public class JGitCommit implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "commit"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Record changes to the repository")
            .addParameter("-m <message>", "Use the given <message> as the commit message")
            .addParameter("-a", "Stage modified and deleted files, then commit")
            .addParameter("--amend", "Amend the previous commit");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        var commitCmd = git.commit();
        var message = "";

        for (int i = 0; i < args.size(); i++)
        {
            var arg = args.get(i);
            if (arg.equals("-m"))
            {
                if (i + 1 < args.size())
                {
                    message = args.get(i + 1);
                    i++;
                }
            }
            else if (arg.equals("-a"))
            {
                commitCmd.setAll(true);
            }
            else if (arg.equals("--amend"))
            {
                commitCmd.setAmend(true);
            }
        }

        if (message.isEmpty())
        {
            commitCmd.setMessage("<no message>");
        }
        else
        {
            commitCmd.setMessage(message);
        }

        var commit = commitCmd.call();
        return new GitCommandResult(0, "[" + git.getRepository().getBranch() + " "
            + commit.getName().substring(0, 7) + "] " + commitCmd.getMessage() + "\n", "");
    }
}
