/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git cherry-pick command implementation
 */
public class JGitCherryPick implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "cherry-pick"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Apply the changes introduced by some existing commits")
            .addParameter("<commit>", "Commit to cherry-pick");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        if (args.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: you must specify a commit to cherry-pick");
        }

        var cherryPickCmd = git.cherryPick();
        for (var commitHash : args)
        {
            var commitRef = git.getRepository().resolve(commitHash);
            if (commitRef == null)
            {
                return new GitCommandResult(1, "", "fatal: bad revision '" + commitHash + "'");
            }
            cherryPickCmd.include(commitRef);
        }

        var result = cherryPickCmd.call();
        if (result.getStatus() == org.eclipse.jgit.api.CherryPickResult.CherryPickStatus.OK)
        {
            return new GitCommandResult(0, "Cherry-pick successful.\n", "");
        }
        else if (result.getStatus() == org.eclipse.jgit.api.CherryPickResult.CherryPickStatus.CONFLICTING)
        {
            return new GitCommandResult(1, "", "Cherry-pick failed due to conflicts. Resolve conflicts and continue.");
        }
        else
        {
            return new GitCommandResult(1, "", "Cherry-pick failed: " + result.getStatus());
        }
    }
}
