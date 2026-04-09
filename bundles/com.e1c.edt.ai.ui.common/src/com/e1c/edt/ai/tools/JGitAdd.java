/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git add command implementation
 */
public class JGitAdd implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "add"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Add file contents to the index")
            .addParameter("<pathspec>...", "Files to add to the staging area")
            .addParameter(".", "Add all files in current directory");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException
    {
        var addCommand = git.add();

        if (args.isEmpty())
        {
            addCommand.addFilepattern(".");
        }
        else
        {
            for (var arg : args)
            {
                addCommand.addFilepattern(arg);
            }
        }

        addCommand.call();
        return new GitCommandResult(0, "", "");
    }
}
