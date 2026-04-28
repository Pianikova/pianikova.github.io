/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.PullCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BranchConfig.BranchRebaseMode;
import org.eclipse.jgit.lib.RepositoryState;

import com.e1c.edt.ai.tools.EditMcpTool;

/**
 * Git pull command implementation
 */
public class JGitPull implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "pull"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Fetch from and integrate with another repository or a local branch")
            .addParameter("<repository>", "Remote repository to pull from (default: origin)")
            .addParameter("<refspec>", "Branch to pull")
            .addParameter("--rebase", "Rebase current branch on top of fetched branch")
            .addParameter("--no-rebase", "Merge instead of rebasing (default)")
            .addParameter("--ff-only", "Refuse to merge unless fast-forward is possible");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException
    {
        var remote = "origin";
        var branch = "";
        Boolean rebase = null;
        var ffOnly = false;

        var positional = 0;
        for (var arg : args)
        {
            if (arg.equals("--rebase"))
            {
                rebase = Boolean.TRUE;
            }
            else if (arg.equals("--no-rebase"))
            {
                rebase = Boolean.FALSE;
            }
            else if (arg.equals("--ff-only"))
            {
                ffOnly = true;
            }
            else if (!arg.startsWith("-"))
            {
                if (positional == 0)
                {
                    remote = arg;
                }
                else if (positional == 1)
                {
                    branch = arg;
                }
                positional++;
            }
        }

        PullCommand pullCmd = git.pull();
        pullCmd.setRemote(remote);
        if (!branch.isEmpty())
        {
            pullCmd.setRemoteBranchName(branch);
        }
        if (rebase != null)
        {
            pullCmd.setRebase(rebase ? BranchRebaseMode.REBASE : BranchRebaseMode.NONE);
        }
        if (ffOnly)
        {
            pullCmd.setRebase(BranchRebaseMode.NONE);
            pullCmd.setFastForward(MergeCommand.FastForwardMode.FF_ONLY);
        }

        var result = pullCmd.call();
        var fetchResult = result.getFetchResult();
        var mergeResult = result.getMergeResult();
        var rebaseResult = result.getRebaseResult();

        var message = new StringBuilder();
        if (fetchResult != null)
        {
            var fetchHead = fetchResult.getAdvertisedRefs();
            if (fetchHead != null && !fetchHead.isEmpty())
            {
                message.append("Fetched ").append(fetchHead.size()).append(" reference(s).\n");
            }
            else
            {
                message.append("Fetch completed.\n");
            }
        }
        if (mergeResult != null)
        {
            var mergeStatus = mergeResult.getMergeStatus();
            if ("ALREADY_UP_TO_DATE".equals(mergeStatus.name()))
            {
                message.append("Already up to date.\n");
            }
            else if (mergeStatus.isSuccessful())
            {
                message.append("Pull successful. Status: ").append(mergeStatus).append("\n");
            }
            else
            {
                var repoState = git.getRepository().getRepositoryState();
                if (repoState == RepositoryState.MERGING)
                {
                    return new GitCommandResult(1, message.toString(),
                        "Merge conflicts occurred. Resolve conflicts using the `" + EditMcpTool.TOOL_NAME + "` tool, "
                            + "then `git add` and `git commit` to complete the merge.");
                }
                return new GitCommandResult(1, message.toString(), "Merge status: " + mergeStatus);
            }
        }
        else if (rebaseResult != null)
        {
            var st = rebaseResult.getStatus();
            if (st.isSuccessful())
            {
                message.append("Rebase successful. Status: ").append(st).append("\n");
            }
            else
            {
                var repoState = git.getRepository().getRepositoryState();
                if (repoState == RepositoryState.REBASING
                    || repoState == RepositoryState.REBASING_INTERACTIVE
                    || repoState == RepositoryState.REBASING_MERGE)
                {
                    return new GitCommandResult(1, message.toString(),
                        "Rebase conflicts occurred. Resolve conflicts using the `" + EditMcpTool.TOOL_NAME + "` tool, "
                            + "then `git add` and `git rebase --continue` (or `--skip` / `--abort`).");
                }
                return new GitCommandResult(1, message.toString(), "Rebase status: " + st);
            }
        }
        else
        {
            message.append("Pull fetch completed (no merge operation performed).\n");
        }
        return new GitCommandResult(0, message.toString(), "");
    }
}
