/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.List;

import org.eclipse.jgit.api.Git;

/**
 * Git ls-remote: list references in a remote repository (without fetching).
 */
public class JGitLsRemote implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "ls-remote"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("List references in a remote repository")
            .addParameter("<remote>", "Remote name or URL (default: origin)")
            .addParameter("-h, --heads", "Show only heads")
            .addParameter("-t, --tags", "Show only tags")
            .addParameter("--refs", "Do not show peeled tag entries")
            .addParameter("<pattern>", "Optional ref name pattern to filter results");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws Exception
    {
        var heads = false;
        var tags = false;
        var refsOnly = false;
        String remote = null;
        String pattern = null;

        for (var arg : args)
        {
            if (arg.equals("-h") || arg.equals("--heads"))
            {
                heads = true;
            }
            else if (arg.equals("-t") || arg.equals("--tags"))
            {
                tags = true;
            }
            else if (arg.equals("--refs"))
            {
                refsOnly = true;
            }
            else if (!arg.startsWith("-"))
            {
                if (remote == null)
                {
                    remote = arg;
                }
                else if (pattern == null)
                {
                    pattern = arg;
                }
            }
        }

        if (remote == null)
        {
            remote = "origin";
        }

        var lsCmd = git.lsRemote().setRemote(remote);
        if (heads || (!heads && !tags))
        {
            lsCmd.setHeads(heads || !tags);
        }
        if (tags || (!heads && !tags))
        {
            lsCmd.setTags(tags || !heads);
        }

        var sb = new StringBuilder();
        for (var ref : lsCmd.call())
        {
            var name = ref.getName();
            if (pattern != null && !name.contains(pattern))
            {
                continue;
            }
            if (refsOnly && name.endsWith("^{}"))
            {
                continue;
            }
            var id = ref.getObjectId();
            if (id == null)
            {
                continue;
            }
            sb.append(id.getName()).append("\t").append(name).append("\n");
        }
        return new GitCommandResult(0, sb.toString(), "");
    }
}
