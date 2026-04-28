/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.transport.URIish;

/**
 * Git remote command implementation
 */
public class JGitRemote implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "remote"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Manage set of tracked repositories")
            .addParameter("add <name> <url>", "Add a remote named <name> with <url>")
            .addParameter("remove <name>", "Remove the remote named <name>")
            .addParameter("rm <name>", "Same as remove")
            .addParameter("-v", "Show remote URLs (verbose)")
            .addParameter("(no arguments)", "List all remotes with their URLs");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, URISyntaxException, IOException
    {
        var add = false;
        var remove = false;
        var show = false;
        var name = "";
        var url = "";

        for (var arg : args)
        {
            if (arg.equals("add"))
            {
                add = true;
            }
            else if (arg.equals("remove") || arg.equals("rm"))
            {
                remove = true;
            }
            else if (arg.equals("show") || arg.equals("-v"))
            {
                show = true;
            }
            else if (!arg.startsWith("-"))
            {
                if (name.isEmpty())
                {
                    name = arg;
                }
                else
                {
                    url = arg;
                }
            }
        }

        if (add && !name.isEmpty() && !url.isEmpty())
        {
            git.remoteAdd().setName(name).setUri(new URIish(url)).call();
            return new GitCommandResult(0, "", "");
        }

        if (remove && !name.isEmpty())
        {
            try
            {
                git.remoteRemove().setRemoteName(name).call();
            }
            catch (Exception e)
            {
                return new GitCommandResult(1, "", "Error removing remote: " + e.getMessage());
            }
            return new GitCommandResult(0, "", "");
        }

        var remotes = git.remoteList().call();
        var sb = new StringBuilder();
        for (var remote : remotes)
        {
            if (!name.isEmpty() && !remote.getName().equals(name))
            {
                continue;
            }

            if (show)
            {
                sb.append(remote.getName()).append("\t").append(remote.getURIs()).append("\n");
            }
            else
            {
                sb.append(remote.getName()).append("\n");
            }
        }

        return new GitCommandResult(0, sb.toString(), "");
    }
}
