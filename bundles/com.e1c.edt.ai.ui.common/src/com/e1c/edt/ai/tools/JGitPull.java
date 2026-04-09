/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

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
            .addParameter("<refspec>", "Branch to pull");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException
    {
        var remote = "origin";
        var branch = "";

        if (!args.isEmpty() && !args.get(0).startsWith("-"))
        {
            remote = args.get(0);
            if (args.size() > 1 && !args.get(1).startsWith("-"))
            {
                branch = args.get(1);
            }
        }

        var pullCmd = git.pull();
        pullCmd.setRemote(remote);
        if (!branch.isEmpty())
        {
            pullCmd.setRemoteBranchName(branch);
        }

        var result = pullCmd.call();
        var fetchResult = result.getFetchResult();
        var mergeResult = result.getMergeResult();

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
                message.append("Merge status: ").append(mergeStatus).append("\n");
            }
        }
        else
        {
            message.append("Pull fetch completed (no merge operation performed).\n");
        }
        return new GitCommandResult(0, message.toString(), "");
    }
}
