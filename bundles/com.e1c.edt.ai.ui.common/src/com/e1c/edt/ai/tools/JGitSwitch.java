/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.RefUpdate;

/**
 * Modern equivalent of `git checkout <branch>`: switches branches.
 */
public class JGitSwitch implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "switch"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Switch branches (modern alternative to `checkout <branch>`)")
            .addParameter("<branch>", "Branch to switch to")
            .addParameter("-c, --create <new-branch>", "Create a new branch and switch to it")
            .addParameter("-C, --force-create <new-branch>", "Force-create (reset if exists) and switch")
            .addParameter("--detach", "Switch to a commit in detached HEAD state")
            .addParameter("-f, --force", "Discard local changes if needed");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws Exception
    {
        String createName = null;
        var forceCreate = false;
        var detach = false;
        var force = false;
        String target = null;

        for (int i = 0; i < args.size(); i++)
        {
            var arg = args.get(i);
            if (arg.equals("-c") || arg.equals("--create"))
            {
                if (i + 1 < args.size())
                {
                    createName = args.get(++i);
                }
            }
            else if (arg.equals("-C") || arg.equals("--force-create"))
            {
                forceCreate = true;
                if (i + 1 < args.size())
                {
                    createName = args.get(++i);
                }
            }
            else if (arg.equals("--detach"))
            {
                detach = true;
            }
            else if (arg.equals("-f") || arg.equals("--force"))
            {
                force = true;
            }
            else if (!arg.startsWith("-"))
            {
                if (target == null)
                {
                    target = arg;
                }
            }
        }

        if (createName == null && target == null)
        {
            return new GitCommandResult(1, "", "fatal: missing branch name");
        }

        try
        {
            var repository = git.getRepository();
            repository.getRefDatabase().refresh();

            if (detach)
            {
                var detachTarget = target != null ? target : "HEAD";
                var targetId = repository.resolve(detachTarget);
                if (targetId == null)
                {
                    return new GitCommandResult(1, "", "fatal: invalid reference: " + detachTarget + "\n");
                }
                git.checkout()
                    .setName(targetId.getName())
                    .setForced(force)
                    .call();
                return new GitCommandResult(0, "HEAD is now at " + targetId.abbreviate(7).name() + "\n", "");
            }

            if (createName != null)
            {
                var startPoint = target != null ? target : "HEAD";
                
                // Check if branch already exists when not using force create
                if (!forceCreate)
                {
                    try
                    {
                        var existingRef = repository.findRef(Constants.R_HEADS + createName);
                        if (existingRef != null)
                        {
                            return new GitCommandResult(1, "", "fatal: A branch named '" + createName + "' already exists\n");
                        }
                    }
                    catch (Exception e)
                    {
                        // Ignore exceptions, branch might not exist
                    }
                }
                
                if (forceCreate)
                {
                    var startId = repository.resolve(startPoint);
                    if (startId == null)
                    {
                        return new GitCommandResult(1, "", "fatal: invalid reference: " + startPoint + "\n");
                    }
                    var refUpdate = repository.updateRef(Constants.R_HEADS + createName);
                    refUpdate.setNewObjectId(startId);
                    refUpdate.setForceUpdate(true);
                    var updateResult = refUpdate.update();
                    if (updateResult == RefUpdate.Result.LOCK_FAILURE || updateResult == RefUpdate.Result.REJECTED
                        || updateResult == RefUpdate.Result.IO_FAILURE)
                    {
                        return new GitCommandResult(1, "",
                            "fatal: cannot reset branch '" + createName + "': " + updateResult + "\n");
                    }
                    repository.getRefDatabase().refresh();
                    git.checkout()
                        .setName(createName)
                        .setForced(force)
                        .call();
                    return new GitCommandResult(0, "Switched to and reset branch '" + createName + "'\n", "");
                }

                git.checkout()
                    .setName(createName)
                    .setCreateBranch(true)
                    .setStartPoint(startPoint)
                    .setForced(force)
                    .call();
                return new GitCommandResult(0, "Switched to a new branch '" + createName + "'\n", "");
            }

            git.checkout()
                .setName(target)
                .setForced(force)
                .call();
            repository.getRefDatabase().refresh();
            var currentBranch = repository.getBranch();
            if (!target.equals(currentBranch))
            {
                return new GitCommandResult(1, "",
                    "fatal: checkout did not switch to '" + target + "' (current branch is '" + currentBranch + "')\n");
            }
            return new GitCommandResult(0, "Switched to branch '" + target + "'\n", "");
        }
        catch (Exception e)
        {
            var message = e.getMessage();
            if (message == null || message.isBlank())
            {
                message = e.getClass().getSimpleName();
            }
            return new GitCommandResult(1, "", "fatal: " + message + "\n");
        }
    }
}
