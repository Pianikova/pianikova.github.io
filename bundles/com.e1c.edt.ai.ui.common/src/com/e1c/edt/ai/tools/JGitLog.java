/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Git log command implementation
 */
public class JGitLog implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "log"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Show commit logs")
            .addParameter("-n<count>", "Limit the number of commits to show (default: 10)")
            .addParameter("--oneline", "Show each commit on a single line");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws IOException, GitAPIException
    {
        var logCmd = git.log();
        var maxCount = 10;
        var oneline = false;

        for (var arg : args)
        {
            if (arg.startsWith("-n"))
            {
                var numStr = arg.substring(2);
                try
                {
                    maxCount = Integer.parseInt(numStr);
                }
                catch (NumberFormatException e)
                {
                    // ignore
                }
            }
            else if (arg.equals("--oneline"))
            {
                oneline = true;
            }
        }

        logCmd.setMaxCount(maxCount);
        var commits = new ArrayList<RevCommit>();
        for (var commit : logCmd.call())
        {
            commits.add(commit);
        }

        var sb = new StringBuilder();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (var commit : commits)
        {
            if (oneline)
            {
                sb.append(commit.abbreviate(7).name()).append(" ").append(commit.getShortMessage()).append("\n");
            }
            else
            {
                sb.append("commit ").append(commit.getName()).append("\n");
                sb.append("Author: ").append(commit.getAuthorIdent().getName()).append(" <")
                    .append(commit.getAuthorIdent().getEmailAddress()).append(">\n");
                sb.append("Date:   ").append(Instant.ofEpochSecond(commit.getCommitTime())
                    .atZone(ZoneId.systemDefault()).format(dateFormatter)).append("\n");
                sb.append("\n    ").append(commit.getFullMessage().trim()).append("\n\n");
            }
        }

        return new GitCommandResult(0, sb.toString(), "");
    }
}
