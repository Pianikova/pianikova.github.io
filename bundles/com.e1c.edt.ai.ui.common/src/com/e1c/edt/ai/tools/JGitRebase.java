/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.RebaseResult.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;

/**
 * Git rebase command implementation with enhanced features
 */
public class JGitRebase implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "rebase"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Reapply commits on top of another base tip")
            .addParameter("<upstream>", "Start point for rebase")
            .addParameter("--continue", "Continue rebase after resolving conflicts")
            .addParameter("--abort", "Abort rebase and restore original branch")
            .addParameter("--skip", "Skip current patch and continue")
            .addParameter("--strategy=<strategy>", "Use the given merge strategy (recursive, resolve, simple, ours, theirs)")
            .addParameter("-i, --interactive", "Make a list and commit a shell script to edit it");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        var rebaseCmd = git.rebase();

        if (args.contains("--continue"))
        {
            return handleContinue(rebaseCmd);
        }
        else if (args.contains("--abort"))
        {
            return handleAbort(rebaseCmd, git.getRepository());
        }
        else if (args.contains("--skip"))
        {
            return handleSkip(rebaseCmd, git.getRepository());
        }
        else
        {
            return handleNewRebase(rebaseCmd, git, args);
        }
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleContinue(RebaseCommand rebaseCmd) throws GitAPIException
    {
        rebaseCmd.setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.CONTINUE);
        RebaseResult result = rebaseCmd.call();

        if (result.getStatus().isSuccessful())
        {
            return new GitCommandResult(0, "Rebase continue successful.\\n", "");
        }
        else if (result.getStatus() == Status.NOTHING_TO_COMMIT)
        {
            return new GitCommandResult(1, "",
                "error: you have unstaged changes. Resolve conflicts and run \"git rebase --continue\".\\n");
        }
        else if (result.getStatus() == Status.STOPPED)
        {
            return new GitCommandResult(1, "",
                "error: could not continue rebase. Resolve conflicts and run \"git rebase --continue\".\\n");
        }
        else if (result.getStatus() == Status.EDIT)
        {
            return new GitCommandResult(1, "",
                "error: cannot continue in edit state. Use \"git rebase --continue\" after making changes.\\n");
        }
        else if (result.getStatus() == Status.FAILED || result.getStatus() == Status.CONFLICTS)
        {
            return new GitCommandResult(1, "",
                "error: rebase failed due to conflicts. Resolve conflicts and run \"git rebase --continue\".\\n");
        }
        else
        {
            return new GitCommandResult(1, "", "Rebase continue failed: " + result.getStatus());
        }
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleAbort(RebaseCommand rebaseCmd, Repository repository) throws GitAPIException, IOException
    {
        if (!isRebaseInProgress(repository))
        {
            return new GitCommandResult(1, "", "fatal: No rebase in progress?\\n");
        }

        rebaseCmd.setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.ABORT);
        RebaseResult result = rebaseCmd.call();

        if (result.getStatus().isSuccessful() || result.getStatus() == Status.ABORTED)
        {
            return new GitCommandResult(0, "Rebase aborted.\\n", "");
        }
        else if (result.getStatus() == Status.FAILED)
        {
            return new GitCommandResult(1, "", "fatal: Failed to abort rebase.\\n");
        }
        else
        {
            return new GitCommandResult(1, "", "Rebase abort failed: " + result.getStatus());
        }
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleSkip(RebaseCommand rebaseCmd, Repository repository) throws GitAPIException, IOException
    {
        if (!isRebaseInProgress(repository))
        {
            return new GitCommandResult(1, "", "fatal: No rebase in progress?\\n");
        }

        rebaseCmd.setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.SKIP);
        RebaseResult result = rebaseCmd.call();

        if (result.getStatus().isSuccessful())
        {
            return new GitCommandResult(0, "Rebase skipped.\\n", "");
        }
        else if (result.getStatus() == Status.NOTHING_TO_COMMIT)
        {
            return new GitCommandResult(1, "", "fatal: Cannot skip - nothing to commit.\\n");
        }
        else if (result.getStatus() == Status.FAILED)
        {
            return new GitCommandResult(1, "", "fatal: Failed to skip commit.\\n");
        }
        else
        {
            return new GitCommandResult(1, "", "Rebase skip failed: " + result.getStatus());
        }
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleNewRebase(RebaseCommand rebaseCmd, Git git, List<String> args) throws GitAPIException, IOException
    {
        if (args.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: you must specify an upstream branch or commit");
        }

        var upstream = args.get(0);
        var currentBranch = git.getRepository().getBranch();
        var upstreamRef = git.getRepository().resolve(upstream);

        if (upstreamRef == null)
        {
            return new GitCommandResult(1, "", "fatal: invalid upstream '" + upstream + "'");
        }

        boolean interactive = args.contains("-i") || args.contains("--interactive");

        rebaseCmd.setUpstream(upstreamRef);

        if (interactive)
        {
            rebaseCmd.setOperation(org.eclipse.jgit.api.RebaseCommand.Operation.BEGIN);
        }

        var result = rebaseCmd.call();

        return handleRebaseResult(result, currentBranch);
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleRebaseResult(RebaseResult result, String currentBranch)
    {
        if (result.getStatus() == Status.STOPPED)
        {
            return new GitCommandResult(1, "",
                "error: could not apply some commits. Resolve conflicts and run \"git rebase --continue\".\\n" +
                "To abort and get back to the state before \"git rebase\", run \"git rebase --abort\".\\n");
        }
        else if (result.getStatus() == Status.EDIT)
        {
            return new GitCommandResult(0,
                "Rebase stopped for editing. Make changes and run \"git rebase --continue\".\\n", "");
        }
        else if (result.getStatus() == Status.INTERACTIVE_PREPARED)
        {
            return new GitCommandResult(0,
                "Interactive rebase prepared. Edit the todo list and run \"git rebase --continue\".\\n", "");
        }
        else if (result.getStatus() == Status.UNCOMMITTED_CHANGES)
        {
            return new GitCommandResult(1, "",
                "error: cannot rebase: you have unstaged changes.\\n");
        }
        else if (result.getStatus() == Status.CONFLICTS)
        {
            return new GitCommandResult(1, "",
                "error: checkout failed due to conflicts.\\n");
        }
        else if (result.getStatus().isSuccessful())
        {
            if (result.getStatus() == Status.UP_TO_DATE)
            {
                return new GitCommandResult(0, "Current branch is up to date.\\n", "");
            }
            if (result.getStatus() == Status.FAST_FORWARD)
            {
                return new GitCommandResult(0, "Fast-forwarded.\\n", "");
            }
            if (result.getStatus() == Status.STASH_APPLY_CONFLICTS)
            {
                return new GitCommandResult(0, "Successfully rebased, but stash apply had conflicts.\\n", "");
            }
            return new GitCommandResult(0,
                "Successfully rebased and updated refs/heads/" + currentBranch + ".\\n", "");
        }
        else
        {
            return new GitCommandResult(1, "", "Rebase failed: " + result.getStatus());
        }
    }

    @SuppressWarnings("nls")
    private boolean isRebaseInProgress(Repository repository) throws IOException
    {
        RepositoryState state = repository.getRepositoryState();

        if (state == RepositoryState.REBASING ||
            state == RepositoryState.REBASING_INTERACTIVE ||
            state == RepositoryState.REBASING_MERGE)
        {
            return true;
        }

        File gitDir = repository.getDirectory();
        if (gitDir != null)
        {
            Path rebaseMergeDir = Paths.get(gitDir.getAbsolutePath(), "rebase-merge");
            Path rebaseApplyDir = Paths.get(gitDir.getAbsolutePath(), "rebase-apply");

            if (Files.exists(rebaseMergeDir) || Files.exists(rebaseApplyDir))
            {
                return true;
            }
        }

        return false;
    }
}
