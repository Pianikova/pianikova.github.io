/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git mv command implementation
 */
public class JGitMv implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "mv"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Move or rename a file, a directory, or a symlink")
            .addParameter("<source> <destination>", "Source and destination paths")
            .addParameter("-f", "Force overwrite if destination exists")
            .addParameter("--force", "Same as -f")
            .addParameter("-n", "Dry run - show what would be done without doing it")
            .addParameter("--dry-run", "Same as -n");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        var force = args.contains("-f") || args.contains("--force");
        var dryRun = args.contains("-n") || args.contains("--dry-run");

        List<String> nonFlagArgs = args.stream()
            .filter(arg -> !arg.equals("-f") && !arg.equals("--force") && !arg.equals("-n") && !arg.equals("--dry-run"))
            .collect(Collectors.toList());

        if (nonFlagArgs.size() < 2)
        {
            return new GitCommandResult(1, "", "fatal: source and destination required");
        }

        var source = nonFlagArgs.get(0);
        var destination = nonFlagArgs.get(1);

        var workTree = git.getRepository().getWorkTree();
        var sourceFile = new File(workTree, source);
        var destFile = new File(workTree, destination);

        if (!sourceFile.exists())
        {
            return new GitCommandResult(1, "", "fatal: '" + source + "' does not exist");
        }

        var actualDestFile = destFile;
        if (destFile.exists() && destFile.isDirectory())
        {
            actualDestFile = new File(destFile, sourceFile.getName());
        }

        if (actualDestFile.exists() && !force)
        {
            return new GitCommandResult(1, "", "fatal: '" + actualDestFile.getName() + "' already exists");
        }

        if (!dryRun)
        {
            if (actualDestFile.exists() && force)
            {
                if (!actualDestFile.delete())
                {
                    return new GitCommandResult(1, "", "fatal: failed to remove destination");
                }
            }

            if (!sourceFile.renameTo(actualDestFile))
            {
                return new GitCommandResult(1, "", "fatal: failed to rename");
            }

            git.rm().addFilepattern(source).call();
            var destPath = actualDestFile.getPath().substring(workTree.getPath().length() + 1).replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
            git.add().addFilepattern(destPath).call();
        }

        var msg = dryRun
            ? "Dry run: Would rename '" + source + "' to '" + destination + "'\n"
            : "Renamed '" + source + "' to '" + destination + "'\n";
        return new GitCommandResult(0, msg, "");
    }
}
