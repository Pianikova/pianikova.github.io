/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git clone command implementation
 * Note: This command is special as it doesn't use an existing Git instance
 */
public class JGitClone implements IJGitCommand
{
    private String workingDirectory;

    public void setWorkingDirectory(String workingDirectory)
    {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public String getName()
    {
        return "clone"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Clone a repository into a new directory")
            .addParameter("<repository>", "The (possibly remote) repository to clone from")
            .addParameter("<directory>", "The name of a new directory to clone into");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException
    {
        if (args.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: you must specify a repository to clone");
        }

        var url = args.get(0);
        File targetDir;
        String dirName;

        if (args.size() > 1)
        {
            dirName = args.get(1);
            targetDir = new File(workingDirectory, dirName);
        }
        else
        {
            File sourcePath = new File(url);
            dirName = sourcePath.getName();
            if (dirName.endsWith(".git"))
            {
                dirName = dirName.substring(0, dirName.length() - 4);
            }
            if (dirName.isEmpty())
            {
                return new GitCommandResult(1, "", "fatal: cannot determine repository name from URL");
            }
            targetDir = new File(workingDirectory, dirName);
        }

        var cloneCmd = Git.cloneRepository();
        cloneCmd.setURI(url);
        cloneCmd.setDirectory(targetDir);
        cloneCmd.call();

        return new GitCommandResult(0, "Cloning into '" + dirName + "'...\ndone.\n", "");
    }
}
