/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;

/**
 * Git describe command implementation
 */
public class JGitDescribe implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "describe"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Give an object a human readable name based on an available ref")
            .addParameter("--all", "Use any ref found in .git/refs/")
            .addParameter("--tags", "Use any tag found in .git/refs/tags")
            .addParameter("--abbrev=<n>", "Abbreviate object name to n digits (default: 7)")
            .addParameter("--dirty", "Append -dirty if working tree is modified");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        var repository = git.getRepository();
        var all = args.contains("--all");
        var tags = args.contains("--tags");
        var abbrev = 7;
        var dirty = args.contains("--dirty");

        for (var arg : args)
        {
            if (arg.startsWith("--abbrev="))
            {
                try
                {
                    abbrev = Integer.parseInt(arg.substring("--abbrev=".length()));
                }
                catch (NumberFormatException e)
                {
                    // ignore
                }
            }
        }

        try
        {
            var headId = repository.resolve(Constants.HEAD);
            try (var revWalk = new org.eclipse.jgit.revwalk.RevWalk(repository))
            {
                var headCommit = revWalk.parseCommit(headId);

                @SuppressWarnings("deprecation")
                var refMap = all ? repository.getAllRefs() : null;
                var tagList = !all ? git.tagList().call() : null;
                String bestTag = null;
                var bestDistance = Integer.MAX_VALUE;

                if (all)
                {
                    for (var ref : refMap.values())
                    {
                        var tagName = ref.getName();

                        // Skip non-refs references
                        if (!tagName.startsWith(Constants.R_HEADS) &&
                            !tagName.startsWith(Constants.R_TAGS) &&
                            !tagName.startsWith(Constants.R_REMOTES))
                        {
                            continue;
                        }

                        try
                        {
                            var tagObjectId = ref.getObjectId();
                            var tagCommit = revWalk.parseCommit(tagObjectId);

                            if (tagCommit.equals(headCommit))
                            {
                                bestDistance = 0;
                                bestTag = tagName;
                                break;
                            }

                            try (var walk = new org.eclipse.jgit.revwalk.RevWalk(repository))
                            {
                                walk.markStart(headCommit);
                                walk.sort(org.eclipse.jgit.revwalk.RevSort.COMMIT_TIME_DESC);
                                var distance = 0;
                                var foundTag = false;
                                try
                                {
                                    for (var commit : walk)
                                    {
                                        if (commit.equals(tagCommit))
                                        {
                                            foundTag = true;
                                            break;
                                        }
                                        distance++;
                                    }
                                }
                                catch (Exception e)
                                {
                                    distance = Integer.MAX_VALUE;
                                }

                                if (foundTag && distance < bestDistance)
                                {
                                    bestDistance = distance;
                                    bestTag = tagName;
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            continue;
                        }
                    }
                }
                else
                {
                    for (var ref : tagList)
                    {
                        var tagName = ref.getName();
                        var isTag = tagName.startsWith(Constants.R_TAGS);

                        if (!isTag)
                        {
                            continue;
                        }

                        // Filter out lightweight tags unless --tags is specified
                        if (!tags)
                        {
                            try
                            {
                                var tagObjectId = ref.getObjectId();
                                var revTag = revWalk.parseAny(tagObjectId);
                                var peeled = revWalk.peel(revTag);
                                if (peeled == null || tagObjectId.equals(peeled.getId()))
                                {
                                    // Not an annotated tag (lightweight tag)
                                    continue;
                                }
                            }
                            catch (Exception e)
                            {
                                continue;
                            }
                        }

                        try
                        {
                            var tagObjectId = ref.getObjectId();
                            var tagCommit = revWalk.parseCommit(tagObjectId);

                            if (tagCommit.equals(headCommit))
                            {
                                bestDistance = 0;
                                bestTag = tagName;
                                break;
                            }

                            try (var walk = new org.eclipse.jgit.revwalk.RevWalk(repository))
                            {
                                walk.markStart(headCommit);
                                walk.sort(org.eclipse.jgit.revwalk.RevSort.COMMIT_TIME_DESC);
                                var distance = 0;
                                var foundTag = false;
                                try
                                {
                                    for (var commit : walk)
                                    {
                                        if (commit.equals(tagCommit))
                                        {
                                            foundTag = true;
                                            break;
                                        }
                                        distance++;
                                    }
                                }
                                catch (Exception e)
                                {
                                    distance = Integer.MAX_VALUE;
                                }

                                if (foundTag && distance < bestDistance)
                                {
                                    bestDistance = distance;
                                    bestTag = tagName;
                                }
                            }
                        }
                        catch (Exception e)
                        {
                            continue;
                        }
                    }
                }

                var sb = new StringBuilder();
                if (bestTag != null)
                {
                    if (bestTag.startsWith(Constants.R_TAGS))
                    {
                        bestTag = bestTag.substring(Constants.R_TAGS.length());
                    }
                    sb.append(bestTag);
                    if (bestDistance > 0)
                    {
                        sb.append("-").append(bestDistance);
                        sb.append("-g").append(headCommit.abbreviate(abbrev).name());
                    }
                }
                else
                {
                    sb.append(headCommit.abbreviate(abbrev).name());
                }

                if (dirty && !git.status().call().isClean())
                {
                    sb.append("-dirty");
                }

                sb.append("\n");
                return new GitCommandResult(0, sb.toString(), "");
            }
        }
        catch (Exception e)
        {
            return new GitCommandResult(1, "", "fatal: cannot describe " + e.getMessage());
        }
    }
}
