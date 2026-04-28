/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.CanonicalTreeParser;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;

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
            .addParameter("--oneline", "Show each commit on a single line")
            .addParameter("-p", "Show patch (diff) introduced by each commit")
            .addParameter("--all", "Include commits from all refs (branches and tags)")
            .addParameter("--grep <pattern>", "Filter commits by message (regex)")
            .addParameter("--author <pattern>", "Filter commits by author (regex over name/email)")
            .addParameter("--since <date>", "Only commits after the given date (yyyy-MM-dd or yyyy-MM-dd HH:mm:ss)")
            .addParameter("--until <date>", "Only commits before the given date")
            .addParameter("--follow <path>", "Follow history of a single file")
            .addParameter("--reverse", "Output commits in reverse order");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws IOException, GitAPIException
    {
        var logCmd = git.log();
        var maxCount = 10;
        var oneline = false;
        var showPatch = false;
        var all = false;
        var reverse = false;
        Pattern grep = null;
        Pattern authorPattern = null;
        Long since = null;
        Long until = null;
        String followPath = null;

        for (int i = 0; i < args.size(); i++)
        {
            var arg = args.get(i);
            if (arg.startsWith("-n"))
            {
                var numStr = arg.length() > 2 ? arg.substring(2)
                    : (i + 1 < args.size() ? args.get(++i) : "");
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
            else if (arg.equals("-p") || arg.equals("--patch"))
            {
                showPatch = true;
            }
            else if (arg.equals("--all"))
            {
                all = true;
            }
            else if (arg.equals("--reverse"))
            {
                reverse = true;
            }
            else if (arg.equals("--grep") && i + 1 < args.size())
            {
                grep = Pattern.compile(args.get(++i));
            }
            else if (arg.startsWith("--grep="))
            {
                grep = Pattern.compile(arg.substring("--grep=".length()));
            }
            else if (arg.equals("--author") && i + 1 < args.size())
            {
                authorPattern = Pattern.compile(args.get(++i));
            }
            else if (arg.startsWith("--author="))
            {
                authorPattern = Pattern.compile(arg.substring("--author=".length()));
            }
            else if (arg.equals("--since") && i + 1 < args.size())
            {
                since = parseDate(args.get(++i));
            }
            else if (arg.startsWith("--since="))
            {
                since = parseDate(arg.substring("--since=".length()));
            }
            else if (arg.equals("--until") && i + 1 < args.size())
            {
                until = parseDate(args.get(++i));
            }
            else if (arg.startsWith("--until="))
            {
                until = parseDate(arg.substring("--until=".length()));
            }
            else if (arg.equals("--follow") && i + 1 < args.size())
            {
                followPath = args.get(++i);
            }
            else if (arg.startsWith("--follow="))
            {
                followPath = arg.substring("--follow=".length());
            }
        }

        if (all)
        {
            var repository = git.getRepository();
            for (Ref ref : git.branchList().setListMode(
                org.eclipse.jgit.api.ListBranchCommand.ListMode.ALL).call())
            {
                var id = ref.getObjectId();
                if (id != null)
                {
                    logCmd.add(id);
                }
            }
            for (Ref tag : repository.getRefDatabase().getRefsByPrefix(
                org.eclipse.jgit.lib.Constants.R_TAGS))
            {
                var id = tag.getObjectId();
                if (id != null)
                {
                    logCmd.add(id);
                }
            }
        }

        if (followPath != null)
        {
            logCmd.addPath(followPath);
        }

        // Don't apply maxCount upstream when filtering — we apply it after filtering.
        var hasFilters = grep != null || authorPattern != null || since != null || until != null;
        if (!hasFilters)
        {
            logCmd.setMaxCount(maxCount);
        }

        var commits = new ArrayList<RevCommit>();
        for (var commit : logCmd.call())
        {
            if (grep != null && !grep.matcher(commit.getFullMessage()).find())
            {
                continue;
            }
            if (authorPattern != null)
            {
                var ident = commit.getAuthorIdent();
                var hay = ident.getName() + " <" + ident.getEmailAddress() + ">";
                if (!authorPattern.matcher(hay).find())
                {
                    continue;
                }
            }
            var time = (long) commit.getCommitTime();
            if (since != null && time < since)
            {
                continue;
            }
            if (until != null && time > until)
            {
                continue;
            }
            commits.add(commit);
            if (commits.size() >= maxCount)
            {
                break;
            }
        }

        if (reverse)
        {
            Collections.reverse(commits);
        }

        var sb = new StringBuilder();
        var dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        var repository = git.getRepository();

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

            if (showPatch)
            {
                try (var revWalk = new RevWalk(repository); var out = new ByteArrayOutputStream();
                    var formatter = new DiffFormatter(out))
                {
                    formatter.setRepository(repository);
                    var newTree = new CanonicalTreeParser();
                    try (var reader = repository.newObjectReader())
                    {
                        newTree.reset(reader, commit.getTree());
                        if (commit.getParentCount() > 0)
                        {
                            var parent = revWalk.parseCommit(commit.getParent(0).getId());
                            var oldTree = new CanonicalTreeParser();
                            oldTree.reset(reader, parent.getTree());
                            for (var entry : formatter.scan(oldTree, newTree))
                            {
                                formatter.format(entry);
                            }
                        }
                        else
                        {
                            for (var entry : formatter.scan(new EmptyTreeIterator(), newTree))
                            {
                                formatter.format(entry);
                            }
                        }
                    }
                    sb.append(out.toString(StandardCharsets.UTF_8));
                    if (!oneline)
                    {
                        sb.append("\n");
                    }
                }
            }
        }

        return new GitCommandResult(0, sb.toString(), "");
    }

    private static Long parseDate(String value)
    {
        if (value == null || value.isBlank())
        {
            return null;
        }
        var trimmed = value.trim();
        try
        {
            return LocalDateTime.parse(trimmed, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                .atZone(ZoneId.systemDefault()).toEpochSecond();
        }
        catch (Exception ignore)
        {
            // try date only
        }
        try
        {
            return LocalDate.parse(trimmed).atStartOfDay(ZoneId.systemDefault()).toEpochSecond();
        }
        catch (Exception ignore)
        {
            // fall through
        }
        try
        {
            return Instant.parse(trimmed).getEpochSecond();
        }
        catch (Exception ignore)
        {
            return null;
        }
    }
}
