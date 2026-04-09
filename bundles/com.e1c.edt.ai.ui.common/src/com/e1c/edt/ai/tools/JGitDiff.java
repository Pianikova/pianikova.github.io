/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.dircache.DirCacheIterator;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.treewalk.EmptyTreeIterator;
import org.eclipse.jgit.treewalk.FileTreeIterator;

import com.google.inject.Inject;

/**
 * Git diff command implementation
 */
public class JGitDiff implements IJGitCommand
{
    private final IJGitCommonHelper commonHelper;

    @Inject
    public JGitDiff(IJGitCommonHelper commonHelper)
    {
        this.commonHelper = commonHelper;
    }

    @Override
    public String getName()
    {
        return "diff"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Show changes between commits, commit and working tree, etc")
            .addParameter("-U<n>", "Generate diffs with <n> lines of context (default: 3)")
            .addParameter("--cached", "View changes you staged for commit")
            .addParameter("--staged", "Same as --cached")
            .addParameter("<path>...", "Limit to specified paths");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws IOException, GitAPIException
    {
        var contextLines = 3;
        var cached = false;
        var paths = new ArrayList<String>();

        for (var arg : args)
        {
            if (arg.startsWith("-U") || arg.startsWith("--unified="))
            {
                var numStr = arg.startsWith("-U") ? arg.substring(2) : arg.substring("--unified=".length());
                try
                {
                    contextLines = Integer.parseInt(numStr);
                }
                catch (NumberFormatException e)
                {
                    // ignore
                }
            }
            else if (arg.equals("--cached") || arg.equals("--staged"))
            {
                cached = true;
            }
            else if (!arg.startsWith("-"))
            {
                paths.add(arg);
            }
        }

        var repository = git.getRepository();
        var outputStream = new ByteArrayOutputStream();
        try (DiffFormatter formatter = new DiffFormatter(outputStream))
        {
            formatter.setRepository(repository);
            formatter.setContext(contextLines);

            var headObjectId = repository.resolve(Constants.HEAD);
            List<DiffEntry> diffs;

            if (cached)
            {
                if (headObjectId == null)
                {
                    diffs = formatter.scan(new EmptyTreeIterator(), new DirCacheIterator(repository.readDirCache()));
                }
                else
                {
                    var headCommit = repository.parseCommit(headObjectId);
                    var headTreeIter = commonHelper.prepareTreeParser(repository, headCommit);
                    diffs = formatter.scan(headTreeIter, new DirCacheIterator(repository.readDirCache()));
                }
            }
            else
            {
                var dirCacheIter = new DirCacheIterator(repository.readDirCache());
                var workingTreeIter = new FileTreeIterator(repository);
                diffs = formatter.scan(dirCacheIter, workingTreeIter);
            }

            for (var diff : diffs)
            {
                if (paths.isEmpty() || paths.stream().anyMatch(p -> diff.getNewPath().contains(p)
                    || diff.getOldPath().contains(p)))
                {
                    formatter.format(diff);
                }
            }
        }

        return new GitCommandResult(0, outputStream.toString(StandardCharsets.UTF_8.name()), "");
    }
}
