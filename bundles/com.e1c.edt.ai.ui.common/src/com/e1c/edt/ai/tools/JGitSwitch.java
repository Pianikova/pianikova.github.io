/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.List;

import org.eclipse.jgit.api.CreateBranchCommand;
import org.eclipse.jgit.api.Git;

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

        var checkout = git.checkout();
        if (createName != null)
        {
            checkout.setCreateBranch(true).setName(createName);
            if (forceCreate)
            {
                checkout.setUpstreamMode(CreateBranchCommand.SetupUpstreamMode.NOTRACK);
                checkout.setForceRefUpdate(true);
            }
            if (target != null)
            {
                checkout.setStartPoint(target);
            }
        }
        else
        {
            checkout.setName(target);
        }
        if (force)
        {
            checkout.setForced(true);
        }
        // detach: when target is a commit (not a branch), checkout already detaches.
        if (detach && target != null)
        {
            checkout.setName(target);
        }
        checkout.call();
        var name = createName != null ? createName : target;
        return new GitCommandResult(0, "Switched to " + (createName != null ? "a new branch '" : "branch '")
            + name + "'\n", "");
    }
}
