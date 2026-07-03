/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.RebaseCommand;
import org.eclipse.jgit.api.RebaseCommand.InteractiveHandler;
import org.eclipse.jgit.api.RebaseResult;
import org.eclipse.jgit.api.RebaseResult.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.RebaseTodoLine;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryState;


/**
 * Git rebase command implementation.
 *
 * NOTE: Interactive rebase (-i) is not supported in an LLM context — there is no
 * editor to drive the todo list. Use --autosquash to automate fixup!/squash!
 * commits, or rewrite history non-interactively via reset --soft + commit --amend
 * + cherry-pick (use the Edit tool to modify files between steps).
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
            .addParameter("--autostash", "Automatically stash uncommitted changes and reapply after rebase")
            .addParameter("--autosquash",
                "Automatically reorder/apply commits prefixed with `fixup!`/`squash!` to amend their target")
            .addParameter("--strategy=<strategy>",
                "Use the given merge strategy (recursive, resolve, simple, ours, theirs)")
            .setNotes("Interactive rebase (-i, --exec, --root, edit/reword/squash) is NOT supported — "
                + "there is no editor available. To rewrite history non-interactively, use one of:\n"
                + "  * --autosquash with `fixup!<subject>` / `squash!<subject>` commit messages\n"
                + "  * `reset --soft <base>` + `commit -m \"...\"` (collapse a range into one commit)\n"
                + "  * `commit --amend -m \"...\"` (reword/fix the latest commit)\n"
                + "  * `cherry-pick` to reapply individual commits onto a new base\n"
                + "Edit files between steps with the `Edit` tool. After conflicts, resolve with `Edit`, "
                + "`add`, then `rebase --continue` (or `--skip` / `--abort`).");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        if (args.contains("-i") || args.contains("--interactive"))
        {
            return new GitCommandResult(1, "",
                "error: interactive rebase (-i) is not supported in this environment.\n"
                    + "Use --autosquash, or rewrite history with reset --soft / commit --amend / cherry-pick.\n");
        }

        var rebaseCmd = git.rebase();

        if (args.contains("--continue"))
        {
            return handleContinue(rebaseCmd);
        }
        if (args.contains("--abort"))
        {
            return handleAbort(rebaseCmd, git.getRepository());
        }
        if (args.contains("--skip"))
        {
            return handleSkip(rebaseCmd, git.getRepository());
        }
        return handleNewRebase(rebaseCmd, git, args);
    }

    private GitCommandResult handleContinue(RebaseCommand rebaseCmd) throws GitAPIException
    {
        rebaseCmd.setOperation(RebaseCommand.Operation.CONTINUE);
        return handleRebaseResult(rebaseCmd.call(), null);
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleAbort(RebaseCommand rebaseCmd, Repository repository)
        throws GitAPIException, IOException
    {
        if (!isRebaseInProgress(repository))
        {
            return new GitCommandResult(1, "", "fatal: No rebase in progress?\n");
        }
        rebaseCmd.setOperation(RebaseCommand.Operation.ABORT);
        var result = rebaseCmd.call();
        if (result.getStatus().isSuccessful() || result.getStatus() == Status.ABORTED)
        {
            return new GitCommandResult(0, "Rebase aborted.\n", "");
        }
        return new GitCommandResult(1, "", "Rebase abort failed: " + result.getStatus());
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleSkip(RebaseCommand rebaseCmd, Repository repository)
        throws GitAPIException, IOException
    {
        if (!isRebaseInProgress(repository))
        {
            return new GitCommandResult(1, "", "fatal: No rebase in progress?\n");
        }
        rebaseCmd.setOperation(RebaseCommand.Operation.SKIP);
        return handleRebaseResult(rebaseCmd.call(), null);
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleNewRebase(RebaseCommand rebaseCmd, Git git, List<String> args)
        throws GitAPIException, IOException
    {
        String upstream = null;
        var autostash = false;
        var autosquash = false;
        for (var arg : args)
        {
            if (arg.equals("--autostash"))
            {
                autostash = true;
            }
            else if (arg.equals("--no-autostash"))
            {
                autostash = false;
            }
            else if (arg.equals("--autosquash"))
            {
                autosquash = true;
            }
            else if (!arg.startsWith("-") && upstream == null)
            {
                upstream = arg;
            }
        }

        if (upstream == null)
        {
            return new GitCommandResult(1, "", "fatal: you must specify an upstream branch or commit");
        }

        var currentBranch = git.getRepository().getBranch();
        var upstreamRef = git.getRepository().resolve(upstream);
        if (upstreamRef == null)
        {
            return new GitCommandResult(1, "", "fatal: invalid upstream '" + upstream + "'");
        }

        // --autostash: stash before rebase, pop after.
        org.eclipse.jgit.revwalk.RevCommit stash = null;
        if (autostash)
        {
            stash = git.stashCreate().setIncludeUntracked(false).call();
        }

        rebaseCmd.setUpstream(upstreamRef);

        if (autosquash)
        {
            rebaseCmd.runInteractively(new AutosquashHandler());
        }

        try
        {
            var result = rebaseCmd.call();
            if (stash != null && (result.getStatus().isSuccessful()))
            {
                git.stashApply().setStashRef(stash.getName()).call();
                git.stashDrop().setStashRef(0).call();
            }
            return handleRebaseResult(result, currentBranch);
        }
        catch (GitAPIException e)
        {
            if (stash != null)
            {
                // Best-effort: leave stash in place for user recovery.
                return new GitCommandResult(1, "",
                    "Rebase failed: " + e.getMessage() + "\n"
                        + "Autostash entry preserved (use `git stash pop` to recover).\n");
            }
            throw e;
        }
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleRebaseResult(RebaseResult result, String currentBranch)
    {
        var status = result.getStatus();
        if (status == Status.STOPPED)
        {
            return new GitCommandResult(1, "",
                "error: could not apply some commits. Resolve conflicts (use `" + EditMcpTool.TOOL_NAME + "`), `add`, "
                    + "then `rebase --continue` (or `--skip` / `--abort`).\n");
        }
        if (status == Status.UNCOMMITTED_CHANGES)
        {
            return new GitCommandResult(1, "",
                "error: cannot rebase: you have unstaged changes. Commit them or use --autostash.\n");
        }
        if (status == Status.CONFLICTS)
        {
            return new GitCommandResult(1, "", "error: checkout failed due to conflicts.\n");
        }
        if (status == Status.NOTHING_TO_COMMIT)
        {
            return new GitCommandResult(1, "",
                "error: no changes - resolve conflicts and run `rebase --continue` (or `--skip`).\n");
        }
        if (status.isSuccessful())
        {
            if (status == Status.UP_TO_DATE)
            {
                return new GitCommandResult(0, "Current branch is up to date.\n", "");
            }
            if (status == Status.FAST_FORWARD)
            {
                return new GitCommandResult(0, "Fast-forwarded.\n", "");
            }
            var ref = currentBranch != null ? "refs/heads/" + currentBranch : "current branch";
            return new GitCommandResult(0, "Successfully rebased and updated " + ref + ".\n", "");
        }
        return new GitCommandResult(1, "", "Rebase failed: " + status);
    }

    @SuppressWarnings("nls")
    private boolean isRebaseInProgress(Repository repository) throws IOException
    {
        var state = repository.getRepositoryState();
        if (state == RepositoryState.REBASING
            || state == RepositoryState.REBASING_INTERACTIVE
            || state == RepositoryState.REBASING_MERGE)
        {
            return true;
        }
        File gitDir = repository.getDirectory();
        if (gitDir != null)
        {
            Path rm = Paths.get(gitDir.getAbsolutePath(), "rebase-merge");
            Path ra = Paths.get(gitDir.getAbsolutePath(), "rebase-apply");
            if (Files.exists(rm) || Files.exists(ra))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * Implements --autosquash by reordering todo lines so that any commit whose
     * subject starts with `fixup!<target>` or `squash!<target>` is moved right
     * after the matching <target> commit and converted to FIXUP/SQUASH actions.
     */
    @SuppressWarnings("nls")
    private static final class AutosquashHandler implements InteractiveHandler
    {
        @Override
        public void prepareSteps(List<RebaseTodoLine> steps)
        {
            var bySubject = new HashMap<String, RebaseTodoLine>();
            for (var s : steps)
            {
                if (s.getAction() == RebaseTodoLine.Action.PICK)
                {
                    var msg = s.getShortMessage();
                    if (msg != null && !msg.startsWith("fixup!") && !msg.startsWith("squash!"))
                    {
                        bySubject.putIfAbsent(msg, s);
                    }
                }
            }

            var reordered = new ArrayList<RebaseTodoLine>(steps.size());
            var consumed = new java.util.HashSet<RebaseTodoLine>();
            for (var s : steps)
            {
                if (consumed.contains(s))
                {
                    continue;
                }
                reordered.add(s);
                if (s.getAction() != RebaseTodoLine.Action.PICK)
                {
                    continue;
                }
                var subject = s.getShortMessage();
                if (subject == null)
                {
                    continue;
                }
                // Append any fixup!/squash! that target this commit's subject.
                for (var f : steps)
                {
                    if (f == s || consumed.contains(f))
                    {
                        continue;
                    }
                    var fmsg = f.getShortMessage();
                    if (fmsg == null)
                    {
                        continue;
                    }
                    String target = null;
                    RebaseTodoLine.Action newAction = null;
                    if (fmsg.startsWith("fixup! "))
                    {
                        target = fmsg.substring("fixup! ".length()).trim();
                        newAction = RebaseTodoLine.Action.FIXUP;
                    }
                    else if (fmsg.startsWith("squash! "))
                    {
                        target = fmsg.substring("squash! ".length()).trim();
                        newAction = RebaseTodoLine.Action.SQUASH;
                    }
                    if (target != null && subject.startsWith(target))
                    {
                        try
                        {
                            f.setAction(newAction);
                        }
                        catch (Exception ignore)
                        {
                            // some JGit versions may forbid certain transitions
                        }
                        reordered.add(f);
                        consumed.add(f);
                    }
                }
            }
            steps.clear();
            steps.addAll(reordered);
        }

        @Override
        public String modifyCommitMessage(String commit)
        {
            return commit;
        }
    }
}
