/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git checkout command implementation
 */
public class JGitCheckout implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "checkout"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Switch branches or restore working tree files")
            .addParameter("<branch>", "Switch to specified branch")
            .addParameter("-b <new_branch>", "Create and checkout a new branch");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        if (args.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: you must specify a branch name");
        }

        var branchOrCommit = args.get(0);
        var createNew = false;
        var branchName = "";

        for (var arg : args)
        {
            if (arg.equals("-b"))
            {
                createNew = true;
            }
        }

        if (createNew && args.size() > 1)
        {
            branchName = args.get(1);
            var startPoint = args.size() > 2 ? args.get(2) : "HEAD";
            git.branchCreate().setName(branchName).setStartPoint(startPoint).call();
            git.checkout().setName(branchName).call();
        }
        else
        {
            var checkoutCmd = git.checkout();
            if (branchOrCommit.startsWith("origin/"))
            {
                var localBranch = branchOrCommit.substring("origin/".length());
                checkoutCmd.setName(localBranch).setCreateBranch(true).setStartPoint(branchOrCommit);
            }
            else
            {
                checkoutCmd.setName(branchOrCommit);
            }
            checkoutCmd.call();
        }

        return new GitCommandResult(0, "Switched to branch '" + (createNew ? branchName : branchOrCommit) + "'\n", "");
    }
}
