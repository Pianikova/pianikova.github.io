/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git blame command implementation
 */
public class JGitBlame implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "blame"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Show what revision and author last modified each line of a file")
            .addParameter("<file>", "File to blame");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        if (args.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: you must specify a file to blame");
        }

        var filePath = args.get(0);
        var blameCmd = git.blame();
        blameCmd.setFilePath(filePath);

        var result = blameCmd.call();
        var sb = new StringBuilder();

        for (int i = 0; i < result.getResultContents().size(); i++)
        {
            var rev = result.getSourceCommit(i);
            var author = rev.getAuthorIdent().getName();
            var time = Instant.ofEpochSecond(rev.getCommitTime())
                .atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_DATE);
            var line = result.getResultContents().getString(i);
            sb.append(String.format("%s (%s %s) %s\n", rev.abbreviate(8).name(), author, time, line));
        }

        return new GitCommandResult(0, sb.toString(), "");
    }
}
