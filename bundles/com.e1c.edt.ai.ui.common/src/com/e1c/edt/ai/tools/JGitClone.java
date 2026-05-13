/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.TagOpt;

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
            .addParameter("<directory>", "The name of a new directory to clone into")
            .addParameter("-b, --branch <name>", "Check out the specified branch after clone")
            .addParameter("--depth <n>", "Create a shallow clone with history truncated to <n> commits")
            .addParameter("--single-branch",
                "Clone only the history leading to the tip of the given branch (use with -b)");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException
    {
        if (args.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: you must specify a repository to clone");
        }

        String url = null;
        String directory = null;
        String branch = null;
        var singleBranch = false;

        for (int i = 0; i < args.size(); i++)
        {
            var arg = args.get(i);
            if ((arg.equals("-b") || arg.equals("--branch")) && i + 1 < args.size())
            {
                branch = args.get(++i);
            }
            else if (arg.startsWith("--branch="))
            {
                branch = arg.substring("--branch=".length());
            }
            else if (arg.equals("--depth") && i + 1 < args.size())
            {
                i++; // consume the depth value but ignore it (not supported by this JGit version)
            }
            else if (arg.startsWith("--depth="))
            {
                // ignore: shallow clone not supported by this JGit version
            }
            else if (arg.equals("--single-branch"))
            {
                singleBranch = true;
            }
            else if (!arg.startsWith("-"))
            {
                if (url == null)
                {
                    url = arg;
                }
                else if (directory == null)
                {
                    directory = arg;
                }
            }
        }

        if (url == null)
        {
            return new GitCommandResult(1, "", "fatal: you must specify a repository to clone");
        }

        String dirName;
        File targetDir;
        if (directory != null)
        {
            dirName = directory;
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
        String uri = url;
        // Convert local file path to file:// URI for JGit
        File sourceFile = new File(url);
        if (sourceFile.exists() && sourceFile.isDirectory())
        {
            uri = sourceFile.toURI().toString();
        }
        cloneCmd.setURI(uri);
        cloneCmd.setDirectory(targetDir);

        if (branch != null)
        {
            cloneCmd.setBranch(branch);
        }
        // Note: shallow clone (--depth) requires a newer JGit version and is not supported here.
        // The clone will proceed as a full clone regardless of --depth.
        if (singleBranch)
        {
            cloneCmd.setCloneAllBranches(false);
            cloneCmd.setTagOption(TagOpt.NO_TAGS);
        }

        try (Git cloned = cloneCmd.call())
        {
            return new GitCommandResult(0, "Cloning into '" + dirName + "'...\ndone.\n", "");
        }
    }
}
