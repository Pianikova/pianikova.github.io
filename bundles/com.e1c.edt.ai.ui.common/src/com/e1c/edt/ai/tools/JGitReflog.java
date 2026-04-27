/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;

/**
 * Git reflog command implementation.
 * Supports: show (default) HEAD or a named ref.
 */
public class JGitReflog implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "reflog"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Show the reference logs (history of ref movements)")
            .addParameter("show <ref>", "Show reflog for the given ref (default: HEAD)")
            .addParameter("-n<count>", "Limit the number of entries (default: all)");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws Exception
    {
        var ref = Constants.HEAD;
        Integer limit = null;
        for (int i = 0; i < args.size(); i++)
        {
            var arg = args.get(i);
            if (arg.equals("show") || arg.equals("--"))
            {
                continue;
            }
            if (arg.startsWith("-n"))
            {
                var n = arg.length() > 2 ? arg.substring(2)
                    : (i + 1 < args.size() ? args.get(++i) : "");
                try
                {
                    limit = Integer.parseInt(n);
                }
                catch (NumberFormatException ignore)
                {
                    // ignore
                }
            }
            else if (!arg.startsWith("-"))
            {
                ref = arg;
            }
        }

        var entries = git.reflog().setRef(ref).call();
        var fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var sb = new StringBuilder();
        var idx = 0;
        for (var e : entries)
        {
            if (limit != null && idx >= limit)
            {
                break;
            }
            var who = e.getWho();
            var time = who != null ? Instant.ofEpochMilli(who.getWhen().getTime())
                .atZone(ZoneId.systemDefault()).format(fmt) : "";
            sb.append(e.getNewId().abbreviate(7).name())
                .append(" ").append(ref).append("@{").append(idx).append("}: ")
                .append(time).append(" ")
                .append(e.getComment() == null ? "" : e.getComment()).append("\n");
            idx++;
        }
        return new GitCommandResult(0, sb.toString(), "");
    }
}
