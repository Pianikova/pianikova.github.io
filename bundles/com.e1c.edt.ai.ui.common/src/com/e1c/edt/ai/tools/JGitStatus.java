/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

import com.google.inject.Inject;

/**
 * Git status command implementation
 */
public class JGitStatus implements IJGitCommand
{
    private final IJGitCommonHelper commonHelper;

    @Inject
    public JGitStatus(IJGitCommonHelper commonHelper)
    {
        this.commonHelper = commonHelper;
    }

    @Override
    public String getName()
    {
        return "status"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Show the working tree status")
            .addParameter("(no arguments)", "Display the status of the working directory");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        var status = git.status().call();
        var sb = new StringBuilder();

        var head = git.getRepository().getBranch();
        sb.append("On branch ").append(head).append("\n");

        var trackingStatus = commonHelper.getBranchTrackingStatus(git, head);
        if (trackingStatus != null)
        {
            if (trackingStatus.getAheadCount() == 0 && trackingStatus.getBehindCount() == 0)
            {
                sb.append("Your branch is up to date with '").append(trackingStatus.getRemoteTrackingBranch()).append("'.\n");
            }
            else if (trackingStatus.getAheadCount() > 0 && trackingStatus.getBehindCount() > 0)
            {
                sb.append("Your branch and '").append(trackingStatus.getRemoteTrackingBranch()).append("' have diverged,\n");
                sb.append("and have ").append(trackingStatus.getAheadCount()).append(" and ")
                    .append(trackingStatus.getBehindCount()).append(" different commits each, respectively.\n");
            }
            else if (trackingStatus.getAheadCount() > 0)
            {
                sb.append("Your branch is ahead of '").append(trackingStatus.getRemoteTrackingBranch()).append("' by ")
                    .append(trackingStatus.getAheadCount()).append(" commit(s).\n");
            }
            else if (trackingStatus.getBehindCount() > 0)
            {
                sb.append("Your branch is behind '").append(trackingStatus.getRemoteTrackingBranch()).append("' by ")
                    .append(trackingStatus.getBehindCount()).append(" commit(s).\n");
            }
        }

        if (status.isClean())
        {
            sb.append("\nnothing to commit, working tree clean\n");
        }
        else
        {
            if (!status.getAdded().isEmpty() || !status.getChanged().isEmpty() || !status.getRemoved().isEmpty())
            {
                sb.append("\nChanges to be committed:\n");
                for (var file : status.getAdded())
                {
                    sb.append("  new file:   ").append(file).append("\n");
                }
                for (var file : status.getChanged())
                {
                    sb.append("  modified:   ").append(file).append("\n");
                }
                for (var file : status.getRemoved())
                {
                    sb.append("  deleted:    ").append(file).append("\n");
                }
            }

            if (!status.getUntracked().isEmpty())
            {
                sb.append("\nUntracked files:\n");
                for (var file : status.getUntracked())
                {
                    sb.append("  ").append(file).append("\n");
                }
            }

            if (!status.getModified().isEmpty() || !status.getMissing().isEmpty())
            {
                sb.append("\nChanges not staged for commit:\n");
                for (var file : status.getModified())
                {
                    sb.append("  modified:   ").append(file).append("\n");
                }
                for (var file : status.getMissing())
                {
                    sb.append("  deleted:    ").append(file).append("\n");
                }
            }

            if (status.getAdded().isEmpty() && status.getChanged().isEmpty() && status.getRemoved().isEmpty())
            {
                sb.append("\nno changes added to commit (use \"git add\" and/or \"git commit -a\")\n");
            }
        }

        return new GitCommandResult(0, sb.toString(), "");
    }
}
