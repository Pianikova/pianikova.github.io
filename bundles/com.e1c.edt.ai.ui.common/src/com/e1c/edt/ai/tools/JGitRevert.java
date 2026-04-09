/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git revert command implementation
 */
public class JGitRevert implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "revert"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Revert some existing commits")
            .addParameter("<commit>", "Commit to revert");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        if (args.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: you must specify a commit to revert");
        }

        var commitHash = args.get(0);
        var commitRef = git.getRepository().resolve(commitHash);

        if (commitRef == null)
        {
            return new GitCommandResult(1, "", "fatal: bad revision '" + commitHash + "'");
        }

        var revertCmd = git.revert();
        revertCmd.include(commitRef);

        try
        {
            var commit = revertCmd.call();
            return new GitCommandResult(0,
                "Revert successful: " + commit.abbreviate(7).name() + " " + commit.getShortMessage() + "\n", "");
        }
        catch (Exception e)
        {
            if (e.getMessage() != null && e.getMessage().contains("conflict"))
            {
                return new GitCommandResult(1, "", "Revert failed due to conflicts. Resolve conflicts and continue.");
            }
            var errorMsg = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return new GitCommandResult(1, "", "fatal: revert failed: " + errorMsg);
        }
    }
}
