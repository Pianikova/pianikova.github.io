/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git push command implementation
 */
public class JGitPush implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "push"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Update remote refs along with associated objects")
            .addParameter("<remote>", "Remote repository name (default: origin)")
            .addParameter("--force", "Force updates even if not fast-forward")
            .addParameter("--all", "Push all branches");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException
    {
        var remote = "origin";
        String branchSpec = null;
        var force = false;
        var all = false;

        var nonOptionIndex = 0;
        for (var arg : args)
        {
            if (arg.equals("-f") || arg.equals("--force"))
            {
                force = true;
            }
            else if (arg.equals("--all"))
            {
                all = true;
            }
            else if (!arg.startsWith("-"))
            {
                if (nonOptionIndex == 0)
                {
                    remote = arg;
                }
                else if (nonOptionIndex == 1)
                {
                    branchSpec = arg;
                }
                nonOptionIndex++;
            }
        }

        var pushCmd = git.push();
        if (branchSpec != null)
        {
            var refSpecString = branchSpec.contains(":") ? branchSpec : branchSpec + ":" + branchSpec;
            pushCmd.add(refSpecString);
        }
        pushCmd.setRemote(remote);
        if (force)
        {
            pushCmd.setForce(true);
        }
        if (all)
        {
            pushCmd.setPushAll();
        }

        var results = pushCmd.call();
        var message = new StringBuilder();
        message.append("To ").append(remote).append("\n");
        for (var result : results)
        {
            var remoteUpdates = result.getRemoteUpdates();
            for (var update : remoteUpdates)
            {
                var newId = update.getNewObjectId();
                if (newId != null)
                {
                    message.append(update.getRemoteName()).append(" -> ").append(newId.abbreviate(7)).append("\n");
                }
            }
        }
        return new GitCommandResult(0, message.toString(), "");
    }
}
