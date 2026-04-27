/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.FetchCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.TagOpt;

/**
 * Git fetch command implementation
 */
public class JGitFetch implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "fetch"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Download objects and refs from another repository")
            .addParameter("<repository>", "Remote repository to fetch from (default: origin)")
            .addParameter("--all", "Fetch from all configured remotes")
            .addParameter("--prune, -p", "Remove remote-tracking refs that no longer exist on the remote")
            .addParameter("--tags", "Fetch all tags");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws Exception
    {
        var remoteArg = "origin";
        var all = false;
        var prune = false;
        var tags = false;
        var explicitRemote = false;

        for (var arg : args)
        {
            if (arg.equals("--all"))
            {
                all = true;
            }
            else if (arg.equals("--prune") || arg.equals("-p"))
            {
                prune = true;
            }
            else if (arg.equals("--tags"))
            {
                tags = true;
            }
            else if (!arg.startsWith("-"))
            {
                if (!explicitRemote)
                {
                    remoteArg = arg;
                    explicitRemote = true;
                }
            }
        }

        var output = new StringBuilder();
        List<String> remotes = new ArrayList<>();
        if (all)
        {
            for (RemoteConfig rc : RemoteConfig.getAllRemoteConfigs(git.getRepository().getConfig()))
            {
                remotes.add(rc.getName());
            }
            if (remotes.isEmpty())
            {
                return new GitCommandResult(0, "No remotes configured.\n", "");
            }
        }
        else
        {
            remotes.add(remoteArg);
        }

        for (var remote : remotes)
        {
            FetchCommand fetchCmd = git.fetch();
            fetchCmd.setRemote(remote);
            fetchCmd.setRefSpecs("refs/heads/*:refs/remotes/" + remote + "/*");
            if (prune)
            {
                fetchCmd.setRemoveDeletedRefs(true);
            }
            if (tags)
            {
                fetchCmd.setTagOpt(TagOpt.FETCH_TAGS);
            }
            fetchCmd.call();
            output.append("From ").append(remote).append("\n");
        }
        return new GitCommandResult(0, output.toString(), "");
    }
}
