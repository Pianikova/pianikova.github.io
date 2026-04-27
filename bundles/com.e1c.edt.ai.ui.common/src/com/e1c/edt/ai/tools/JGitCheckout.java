/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.CheckoutCommand.Stage;
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
            .addParameter("-b <new_branch>", "Create and checkout a new branch")
            .addParameter("-B <new_branch>", "Create or reset a branch and check it out")
            .addParameter("-f, --force", "Force checkout (discard local modifications)")
            .addParameter("-- <pathspec>...", "Restore files from the index or given source")
            .addParameter("--theirs", "When checking out paths from index with conflicts, take 'theirs'")
            .addParameter("--ours", "When checking out paths from index with conflicts, take 'ours'")
            .setNotes("To switch branches prefer the modern `switch` command. "
                + "To restore files prefer `restore`.");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        if (args.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: you must specify a branch name");
        }

        var createNew = false;
        var forceCreate = false;
        var force = false;
        Stage stage = null;
        String branchName = null;
        String startPoint = null;
        var paths = new ArrayList<String>();
        var afterDoubleDash = false;
        String target = null;

        for (int i = 0; i < args.size(); i++)
        {
            var arg = args.get(i);
            if (afterDoubleDash)
            {
                paths.add(arg);
                continue;
            }
            if (arg.equals("--"))
            {
                afterDoubleDash = true;
            }
            else if (arg.equals("-b") && i + 1 < args.size())
            {
                createNew = true;
                branchName = args.get(++i);
            }
            else if (arg.equals("-B") && i + 1 < args.size())
            {
                createNew = true;
                forceCreate = true;
                branchName = args.get(++i);
            }
            else if (arg.equals("-f") || arg.equals("--force"))
            {
                force = true;
            }
            else if (arg.equals("--theirs"))
            {
                stage = Stage.THEIRS;
            }
            else if (arg.equals("--ours"))
            {
                stage = Stage.OURS;
            }
            else if (!arg.startsWith("-"))
            {
                if (createNew && startPoint == null && branchName != null)
                {
                    startPoint = arg;
                }
                else if (target == null)
                {
                    target = arg;
                }
                else
                {
                    paths.add(arg);
                }
            }
        }

        // File-level checkout (`git checkout -- <path>` or `git checkout <rev> -- <path>`)
        if (!paths.isEmpty() || (target != null && afterDoubleDash))
        {
            var checkoutCmd = git.checkout();
            for (var p : paths)
            {
                checkoutCmd.addPath(p);
            }
            if (target != null && afterDoubleDash)
            {
                checkoutCmd.setStartPoint(target);
            }
            if (stage != null)
            {
                checkoutCmd.setStage(stage);
            }
            checkoutCmd.setForced(force);
            checkoutCmd.call();
            return new GitCommandResult(0, "", "");
        }

        if (createNew && branchName != null)
        {
            if (forceCreate)
            {
                // -B: delete existing then create
                try
                {
                    git.branchDelete().setBranchNames(branchName).setForce(true).call();
                }
                catch (Exception ignore)
                {
                    // branch might not exist
                }
            }
            git.checkout()
                .setName(branchName)
                .setCreateBranch(true)
                .setStartPoint(startPoint != null ? startPoint : "HEAD")
                .setForced(force)
                .call();
            return new GitCommandResult(0, "Switched to a new branch '" + branchName + "'\n", "");
        }

        if (target == null)
        {
            return new GitCommandResult(1, "", "fatal: you must specify a branch name");
        }

        var checkoutCmd = git.checkout();
        if (target.startsWith("origin/"))
        {
            var localBranch = target.substring("origin/".length());
            checkoutCmd.setName(localBranch).setCreateBranch(true).setStartPoint(target);
        }
        else
        {
            checkoutCmd.setName(target);
        }
        checkoutCmd.setForced(force);
        var ref = checkoutCmd.call();
        return new GitCommandResult(0, "Switched to branch '" + ref.getName() + "'\n", "");
    }
}
