/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.CherryPickResult;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.RepositoryState;

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
            .addParameter("<commit>...", "Commits to cherry-pick (one or more)")
            .addParameter("-n, --no-commit", "Apply changes without making a commit")
            .addParameter("--continue", "Continue after resolving conflicts")
            .addParameter("--abort", "Abort and restore the original branch state")
            .addParameter("--skip", "Skip the current commit and continue");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        if (args.contains("--continue"))
        {
            return handleContinue(git);
        }
        if (args.contains("--abort"))
        {
            return handleAbort(git);
        }
        if (args.contains("--skip"))
        {
            return handleSkip(git);
        }

        var noCommit = false;
        var commits = new ArrayList<String>();

        for (var arg : args)
        {
            if (arg.equals("-n") || arg.equals("--no-commit"))
            {
                noCommit = true;
            }
            else if (!arg.startsWith("-"))
            {
                commits.add(arg);
            }
        }

        if (commits.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: you must specify a commit to cherry-pick");
        }

        var cherryPickCmd = git.cherryPick();
        cherryPickCmd.setNoCommit(noCommit);
        for (var commitHash : commits)
        {
            var commitRef = git.getRepository().resolve(commitHash);
            if (commitRef == null)
            {
                return new GitCommandResult(1, "", "fatal: bad revision '" + commitHash + "'");
            }
            cherryPickCmd.include(commitRef);
        }

        var result = cherryPickCmd.call();
        return interpretResult(result);
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleContinue(Git git) throws GitAPIException, IOException
    {
        var state = git.getRepository().getRepositoryState();
        if (state != RepositoryState.CHERRY_PICKING && state != RepositoryState.CHERRY_PICKING_RESOLVED)
        {
            return new GitCommandResult(1, "", "fatal: no cherry-pick in progress\n");
        }
        // Resume by committing: create a commit using the current index state.
        git.commit().setAllowEmpty(false).call();
        return new GitCommandResult(0, "Cherry-pick continued successfully.\n", "");
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleAbort(Git git) throws GitAPIException, IOException
    {
        var state = git.getRepository().getRepositoryState();
        if (state != RepositoryState.CHERRY_PICKING && state != RepositoryState.CHERRY_PICKING_RESOLVED)
        {
            return new GitCommandResult(1, "", "fatal: no cherry-pick in progress\n");
        }
        git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("ORIG_HEAD").call();
        return new GitCommandResult(0, "Cherry-pick aborted.\n", "");
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleSkip(Git git) throws GitAPIException, IOException
    {
        var state = git.getRepository().getRepositoryState();
        if (state != RepositoryState.CHERRY_PICKING && state != RepositoryState.CHERRY_PICKING_RESOLVED)
        {
            return new GitCommandResult(1, "", "fatal: no cherry-pick in progress\n");
        }
        // Skip: reset to HEAD (discard current conflict), then mark as done.
        git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("HEAD").call();
        // Clear the CHERRY_PICK_HEAD
        var cherryPickHead = new java.io.File(git.getRepository().getDirectory(), "CHERRY_PICK_HEAD");
        if (cherryPickHead.exists())
        {
            cherryPickHead.delete();
        }
        return new GitCommandResult(0, "Skipped cherry-pick.\n", "");
    }

    @SuppressWarnings("nls")
    private static GitCommandResult interpretResult(CherryPickResult result)
    {
        switch (result.getStatus())
        {
            case OK:
                return new GitCommandResult(0, "Cherry-pick successful.\n", "");
            case CONFLICTING:
                return new GitCommandResult(1, "",
                    "Cherry-pick stopped due to conflicts. Resolve conflicts using the `Edit` tool, "
                        + "then `add` and `cherry-pick --continue` (or `--skip` / `--abort`).\n");
            case FAILED:
                var failures = result.getFailingPaths();
                var detail = failures != null ? failures.toString() : "unknown";
                return new GitCommandResult(1, "", "Cherry-pick failed: " + detail + "\n");
            default:
                return new GitCommandResult(1, "", "Cherry-pick failed: " + result.getStatus() + "\n");
        }
    }
}
