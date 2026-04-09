/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;

/**
 * Git tag command implementation
 */
public class JGitTag implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "tag"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Create, list, delete or verify a tag object signed with GPG")
            .addParameter("<tagname>", "Create a tag with this name")
            .addParameter("-a", "Annotated tag")
            .addParameter("--annotate", "Same as -a")
            .addParameter("-m <msg>", "Message for the tag")
            .addParameter("(no arguments)", "List all tags");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException
    {
        if (args.isEmpty())
        {
            var tags = git.tagList().call();
            var sb = new StringBuilder();
            for (var tag : tags)
            {
                var name = tag.getName();
                if (name.startsWith(Constants.R_TAGS))
                {
                    name = name.substring(Constants.R_TAGS.length());
                }
                sb.append(name).append("\n");
            }
            return new GitCommandResult(0, sb.toString(), "");
        }

        var message = "";
        var annotated = false;
        String tag = null;
        var skipNext = false;

        for (var i = 0; i < args.size(); i++)
        {
            if (skipNext)
            {
                skipNext = false;
                continue;
            }

            var arg = args.get(i);

            if (arg.equals("-a") || arg.equals("--annotate"))
            {
                annotated = true;
            }
            else if (arg.equals("-m") && i + 1 < args.size())
            {
                message = args.get(i + 1);
                skipNext = true;
            }
            else if (tag == null && !arg.startsWith("-"))
            {
                tag = arg;
            }
        }

        if (tag == null)
        {
            return new GitCommandResult(1, "", "error: tag name required\n");
        }

        var tagCmd = git.tag();
        tagCmd.setName(tag);
        if (annotated)
        {
            tagCmd.setMessage(message.isEmpty() ? tag : message);
        }
        tagCmd.call();

        return new GitCommandResult(0, "", "");
    }
}
