/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

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
            .addParameter("<repository>", "Remote repository to fetch from (default: origin)");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException
    {
        var remote = "origin";
        if (!args.isEmpty() && !args.get(0).startsWith("-"))
        {
            remote = args.get(0);
        }

        var fetchCmd = git.fetch();
        fetchCmd.setRemote(remote);
        fetchCmd.setRefSpecs("refs/heads/*:refs/remotes/" + remote + "/*");
        @SuppressWarnings("unused")
        var results = fetchCmd.call();
        return new GitCommandResult(0, "From " + remote + "\n", "");
    }
}
