/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.RepositoryState;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;

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
            .addParameter("<commit>...", "Commits to revert (one or more)")
            .addParameter("-n, --no-commit", "Apply revert changes to the index/working tree without committing")
            .addParameter("-m <parent-number>",
                "For a merge commit, select the parent branch (1-based) to revert with respect to")
            .addParameter("--continue", "Continue after resolving conflicts")
            .addParameter("--abort", "Abort the revert and restore the original branch state")
            .addParameter("--skip", "Skip the current commit and continue the revert sequence");
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
        Integer mainline = null;
        var commits = new ArrayList<String>();

        for (int i = 0; i < args.size(); i++)
        {
            var arg = args.get(i);
            if (arg.equals("-n") || arg.equals("--no-commit"))
            {
                noCommit = true;
            }
            else if (arg.equals("-m") && i + 1 < args.size())
            {
                try
                {
                    mainline = Integer.parseInt(args.get(++i));
                }
                catch (NumberFormatException ignore)
                {
                    // ignore
                }
            }
            else if (!arg.startsWith("-"))
            {
                commits.add(arg);
            }
        }

        if (commits.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: you must specify a commit to revert");
        }

        var sb = new StringBuilder();
        for (var commitHash : commits)
        {
            var commitRef = git.getRepository().resolve(commitHash);
            if (commitRef == null)
            {
                return new GitCommandResult(1, "", "fatal: bad revision '" + commitHash + "'");
            }

            try (RevWalk revWalk = new RevWalk(git.getRepository()))
            {
                RevCommit commit = revWalk.parseCommit(commitRef);
                if (commit.getParentCount() > 1)
                {
                    // Merge commit - revert not supported by JGit
                    return new GitCommandResult(1, "", "fatal: revert of merge commits is not supported by JGit API. "
                        + "Use native git or manually revert changes.\n");
                }
            }

            var revertCmd = git.revert();
            revertCmd.include(commitRef);

            try
            {
                var resultCommit = revertCmd.call();
                if (resultCommit == null)
                {
                    // Conflicts occurred
                    return new GitCommandResult(1, "",
                        "Revert stopped due to conflicts. Resolve conflicts using the `Edit` tool, "
                            + "then `add` and `revert --continue` (or `--skip` / `--abort`).\n");
                }
                if (noCommit)
                {
                    // --no-commit: revert made a commit, undo it with reset --soft so changes stay staged
                    git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.SOFT).setRef("HEAD~1").call();
                }
                else
                {
                    sb.append("[").append(git.getRepository().getBranch()).append(" ")
                        .append(resultCommit.abbreviate(7).name()).append("] ")
                        .append(resultCommit.getShortMessage()).append("\n");
                }
            }
            catch (Exception e)
            {
                var msg = e.getMessage();
                if (msg != null && msg.toLowerCase().contains("conflict"))
                {
                    return new GitCommandResult(1, "",
                        "Revert of '" + commitHash + "' failed due to conflicts. "
                            + "Resolve conflicts, then `revert --continue`.\n");
                }
                return new GitCommandResult(1, "", "fatal: revert failed: " + (msg != null ? msg : e.getClass().getSimpleName()) + "\n");
            }
        }

        if (noCommit)
        {
            return new GitCommandResult(0, "Reverted changes applied to index.\n", "");
        }
        return new GitCommandResult(0, sb.toString(), "");
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleContinue(Git git) throws GitAPIException, IOException
    {
        var state = git.getRepository().getRepositoryState();
        if (state != RepositoryState.REVERTING && state != RepositoryState.REVERTING_RESOLVED)
        {
            return new GitCommandResult(1, "", "fatal: no revert in progress\n");
        }

        // Read commit message from MERGE_MSG file created by JGit
        var mergeMsgFile = new File(git.getRepository().getDirectory(), "MERGE_MSG");
        var commitCmd = git.commit().setAllowEmpty(false);
        if (mergeMsgFile.exists())
        {
            try (var reader = new java.io.BufferedReader(new java.io.FileReader(mergeMsgFile)))
            {
                var msg = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null)
                {
                    msg.append(line).append("\n");
                }
                commitCmd.setMessage(msg.toString());
            }
        }
        else
        {
            commitCmd.setMessage("Revert completed after conflict resolution\n");
        }

        commitCmd.call();
        return new GitCommandResult(0, "Revert continued successfully.\n", "");
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleAbort(Git git) throws GitAPIException, IOException
    {
        var state = git.getRepository().getRepositoryState();
        if (state != RepositoryState.REVERTING && state != RepositoryState.REVERTING_RESOLVED)
        {
            return new GitCommandResult(1, "", "fatal: no revert in progress\n");
        }
        // JGit doesn't create ORIG_HEAD for revert, so we reset to HEAD to clear conflicts
        git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("HEAD").call();
        var revertHead = new File(git.getRepository().getDirectory(), "REVERT_HEAD");
        if (revertHead.exists())
        {
            revertHead.delete();
        }
        var mergeMsg = new File(git.getRepository().getDirectory(), "MERGE_MSG");
        if (mergeMsg.exists())
        {
            mergeMsg.delete();
        }
        return new GitCommandResult(0, "Revert aborted.\n", "");
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleSkip(Git git) throws GitAPIException, IOException
    {
        var state = git.getRepository().getRepositoryState();
        if (state != RepositoryState.REVERTING && state != RepositoryState.REVERTING_RESOLVED)
        {
            return new GitCommandResult(1, "", "fatal: no revert in progress\n");
        }
        git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("HEAD").call();
        var revertHead = new File(git.getRepository().getDirectory(), "REVERT_HEAD");
        if (revertHead.exists())
        {
            revertHead.delete();
        }
        return new GitCommandResult(0, "Skipped revert.\n", "");
    }
}
