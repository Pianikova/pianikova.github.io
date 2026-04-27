/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.InitCommand;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git init command implementation.
 * Special: does not require an existing repository — handled like clone in {@link JGitMcpTool}.
 */
public class JGitInit implements IJGitCommand
{
    private String workingDirectory;

    public void setWorkingDirectory(String workingDirectory)
    {
        this.workingDirectory = workingDirectory;
    }

    @Override
    public String getName()
    {
        return "init"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Create an empty Git repository or reinitialize an existing one")
            .addParameter("<directory>", "Directory to init (default: working directory)")
            .addParameter("--bare", "Create a bare repository")
            .addParameter("-b <name>, --initial-branch=<name>", "Set the initial branch name (default: master)");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException
    {
        var bare = false;
        String initialBranch = null;
        String directory = null;

        for (int i = 0; i < args.size(); i++)
        {
            var arg = args.get(i);
            if (arg.equals("--bare"))
            {
                bare = true;
            }
            else if (arg.equals("-b") || arg.equals("--initial-branch"))
            {
                if (i + 1 < args.size())
                {
                    initialBranch = args.get(++i);
                }
            }
            else if (arg.startsWith("--initial-branch="))
            {
                initialBranch = arg.substring("--initial-branch=".length());
            }
            else if (!arg.startsWith("-"))
            {
                directory = arg;
            }
        }

        File targetDir;
        if (directory == null || directory.isBlank())
        {
            if (workingDirectory == null || workingDirectory.isBlank())
            {
                return new GitCommandResult(1, "", "fatal: no directory specified");
            }
            targetDir = new File(workingDirectory);
        }
        else
        {
            var d = new File(directory);
            targetDir = d.isAbsolute() ? d : new File(workingDirectory, directory);
        }
        if (!targetDir.exists() && !targetDir.mkdirs())
        {
            return new GitCommandResult(1, "", "fatal: cannot create directory '" + targetDir.getPath() + "'");
        }

        InitCommand initCmd = Git.init();
        initCmd.setDirectory(targetDir);
        initCmd.setBare(bare);
        if (initialBranch != null && !initialBranch.isBlank())
        {
            initCmd.setInitialBranch(initialBranch);
        }
        try (var created = initCmd.call())
        {
            var msg = (bare ? "Initialized empty Git repository in " : "Initialized empty Git repository in ")
                + created.getRepository().getDirectory().getAbsolutePath() + "\n";
            return new GitCommandResult(0, msg, "");
        }
    }
}
