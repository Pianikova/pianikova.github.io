/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git ls-files command implementation
 */
public class JGitLsFiles implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "ls-files"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Show information about files in the index")
            .addParameter("-c", "Show cached files in the index")
            .addParameter("--cached", "Same as -c")
            .addParameter("-d", "Show deleted files")
            .addParameter("--deleted", "Same as -d")
            .addParameter("-m", "Show modified files")
            .addParameter("--modified", "Same as -m")
            .addParameter("-o", "Show other (i.e., untracked) files")
            .addParameter("--others", "Same as -o")
            .addParameter("-s", "Show staged contents' mode bits, object name and stage number")
            .addParameter("--stage", "Same as -s");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        var repository = git.getRepository();
        var staged = args.contains("--stage") || args.contains("-s");
        var cached = args.contains("--cached") || args.contains("-c");
        var deleted = args.contains("--deleted") || args.contains("-d");
        var modified = args.contains("--modified") || args.contains("-m");
        var others = args.contains("--others") || args.contains("-o");

        var index = repository.readDirCache();
        var sb = new StringBuilder();

        if (staged)
        {
            for (int i = 0; i < index.getEntryCount(); i++)
            {
                var entry = index.getEntry(i);
                sb.append(String.format("%06o %s %s\t%s\n", entry.getFileMode().getBits(),
                    entry.getObjectId().abbreviate(7).name(), entry.getStage() > 0 ? entry.getStage() : "0",
                    entry.getPathString()));
            }
        }
        else
        {
            var status = git.status().call();
            var showOthers = others;
            var showDeleted = deleted;
            var showModified = modified;
            var showCached = cached;

            if (!showOthers && !showDeleted && !showModified && !showCached)
            {
                showCached = true;
            }

            if (showOthers)
            {
                for (var file : status.getUntracked())
                {
                    sb.append(file).append("\n");
                }
            }

            if (showDeleted || showModified)
            {
                for (var file : status.getMissing())
                {
                    if (showDeleted && !showModified)
                    {
                        sb.append(file).append("\n");
                    }
                    else if (showModified)
                    {
                        sb.append(file).append("\n");
                    }
                }
            }

            if (showModified)
            {
                for (var file : status.getModified())
                {
                    sb.append(file).append("\n");
                }
            }

            if (showCached)
            {
                for (int i = 0; i < index.getEntryCount(); i++)
                {
                    var entry = index.getEntry(i);
                    var path = entry.getPathString();
                    if (!showDeleted || !status.getMissing().contains(path))
                    {
                        sb.append(path).append("\n");
                    }
                }
            }
        }

        return new GitCommandResult(0, sb.toString(), "");
    }
}
