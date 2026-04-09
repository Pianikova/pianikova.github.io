/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git clean command implementation
 */
public class JGitClean implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "clean"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Remove untracked files from the working tree")
            .addParameter("-n", "Dry run - show what would be removed without actually removing")
            .addParameter("--dry-run", "Same as -n")
            .addParameter("-f", "Force removal")
            .addParameter("--force", "Same as -f")
            .addParameter("-d", "Remove untracked directories")
            .addParameter("-x", "Remove ignored files");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws IOException, GitAPIException
    {
        var dryRun = args.contains("-n") || args.contains("--dry-run");
        var force = args.contains("-f") || args.contains("--force");
        var directories = args.contains("-d");

        var status = git.status().call();
        var untrackedFiles = status.getUntracked();
        var sb = new StringBuilder();

        if (dryRun)
        {
            sb.append("Would remove the following items:\n");
            for (var file : untrackedFiles)
            {
                if (directories || !file.endsWith("/"))
                {
                    sb.append("  ").append(file).append("\n");
                }
            }
        }
        else if (force)
        {
            var workDir = git.getRepository().getWorkTree();
            var removed = 0;
            var directoriesToDelete = new ArrayList<File>();
            
            for (var file : untrackedFiles)
            {
                if (directories || !file.endsWith("/"))
                {
                    var filePath = new File(workDir, file);
                    if (filePath.delete())
                    {
                        removed++;
                        if (directories)
                        {
                            directoriesToDelete.add(filePath.getParentFile());
                        }
                    }
                }
            }
            
            if (directories)
            {
                for (var dir : directoriesToDelete)
                {
                    deleteEmptyDirectory(dir);
                }
            }
            
            sb.append("Removed ").append(removed).append(" files.\n");
        }
        else
        {
            sb.append("Use -f/--force to actually remove files.\n");
        }

        return new GitCommandResult(0, sb.toString(), "");
    }

    private void deleteEmptyDirectory(File dir)
    {
        if (dir == null || !dir.isDirectory())
        {
            return;
        }
        var files = dir.listFiles();
        if (files == null || files.length == 0)
        {
            dir.delete();
        }
    }
}
