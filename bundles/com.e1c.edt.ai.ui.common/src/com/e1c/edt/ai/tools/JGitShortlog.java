/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;

/**
 * Git shortlog: summarize git log output, grouped by author.
 */
public class JGitShortlog implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "shortlog"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Summarize git log output, grouped by author")
            .addParameter("<revision-range>", "Range to summarize (default: HEAD)")
            .addParameter("-n, --numbered", "Sort output by number of commits per author")
            .addParameter("-s, --summary", "Show only commit count per author (no subjects)")
            .addParameter("-e, --email", "Include author email next to author name");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws Exception
    {
        var numbered = false;
        var summary = false;
        var email = false;
        String range = null;

        for (var arg : args)
        {
            if (arg.equals("-n") || arg.equals("--numbered"))
            {
                numbered = true;
            }
            else if (arg.equals("-s") || arg.equals("--summary"))
            {
                summary = true;
            }
            else if (arg.equals("-e") || arg.equals("--email"))
            {
                email = true;
            }
            else if (!arg.startsWith("-"))
            {
                range = arg;
            }
        }

        var logCmd = git.log();
        if (range != null)
        {
            if (range.contains(".."))
            {
                var parts = range.split("\\.\\.", 2);
                var from = parts[0].isEmpty() ? null : git.getRepository().resolve(parts[0]);
                var to = parts[1].isEmpty() ? git.getRepository().resolve("HEAD")
                    : git.getRepository().resolve(parts[1]);
                if (to == null)
                {
                    return new GitCommandResult(128, "", "fatal: bad revision '" + range + "'");
                }
                if (from != null)
                {
                    logCmd.addRange(from, to);
                }
                else
                {
                    logCmd.add(to);
                }
            }
            else
            {
                ObjectId id = git.getRepository().resolve(range);
                if (id == null)
                {
                    return new GitCommandResult(128, "", "fatal: bad revision '" + range + "'");
                }
                logCmd.add(id);
            }
        }

        var byAuthor = new LinkedHashMap<String, java.util.List<String>>();
        for (var commit : logCmd.call())
        {
            var who = commit.getAuthorIdent();
            var key = email ? who.getName() + " <" + who.getEmailAddress() + ">" : who.getName();
            byAuthor.computeIfAbsent(key, k -> new java.util.ArrayList<>())
                .add(commit.getShortMessage());
        }

        var entries = new java.util.ArrayList<>(byAuthor.entrySet());
        if (numbered)
        {
            entries.sort((a, b) -> Integer.compare(b.getValue().size(), a.getValue().size()));
        }

        var sb = new StringBuilder();
        for (var e : entries)
        {
            if (summary)
            {
                sb.append(e.getValue().size()).append("\t").append(e.getKey()).append("\n");
            }
            else
            {
                sb.append(e.getKey()).append(" (").append(e.getValue().size()).append("):\n");
                for (var subject : e.getValue())
                {
                    sb.append("      ").append(subject).append("\n");
                }
                sb.append("\n");
            }
        }
        return new GitCommandResult(0, sb.toString(), "");
    }
}
