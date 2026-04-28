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
        var ignored = args.contains("-x");

        var status = git.status().call();
        var untrackedFiles = new ArrayList<String>(status.getUntracked());
        if (directories)
        {
            untrackedFiles.addAll(status.getUntrackedFolders());
        }
        
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
            if (ignored)
            {
                var ignoredFiles = new ArrayList<String>(status.getIgnoredNotInIndex());
                for (var file : ignoredFiles)
                {
                    if (directories || !file.endsWith("/"))
                    {
                        sb.append("  ").append(file).append("\n");
                    }
                }
            }
        }
        else if (force)
        {
            var workDir = git.getRepository().getWorkTree();
            var removed = 0;
            var directoriesToDelete = new ArrayList<File>();
            
            var filesToDelete = new ArrayList<String>(untrackedFiles);
            if (ignored)
            {
                filesToDelete.addAll(status.getIgnoredNotInIndex());
            }
            
            for (var file : filesToDelete)
            {
                if (directories || !file.endsWith("/"))
                {
                    var filePath = new File(workDir, file);
                    if (filePath.isDirectory() ? deleteDirectory(filePath) : filePath.delete())
                    {
                        removed++;
                        if (directories && filePath.isDirectory())
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
            
            sb.append("Removed ").append(removed).append(" items.\n");
        }
        else
        {
            return new GitCommandResult(1, "", "fatal: clean.requireForce defaults to true and neither -i, -n, nor -f given; refusing to clean\n");
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

    private boolean deleteDirectory(File dir)
    {
        var files = dir.listFiles();
        if (files != null)
        {
            for (var file : files)
            {
                var deleted = file.isDirectory() ? deleteDirectory(file) : file.delete();
                if (!deleted)
                {
                    return false;
                }
            }
        }
        return dir.delete();
    }
}
