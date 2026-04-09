/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.merge.MergeStrategy;

/**
 * Git merge command implementation
 */
public class JGitMerge implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "merge"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Join two or more development histories together")
            .addParameter("<commit>", "Branch or commit to merge into current branch");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args)
        throws GitAPIException, AmbiguousObjectException, IncorrectObjectTypeException, IOException
    {
        if (args.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: you must specify a branch to merge");
        }

        var branch = args.get(0);
        var ref = git.getRepository().resolve(branch);
        if (ref == null)
        {
            return new GitCommandResult(1, "", "fatal: " + branch + " - not found");
        }
        var mergeCmd = git.merge();
        mergeCmd.include(ref);
        mergeCmd.setStrategy(MergeStrategy.RECURSIVE);

        var result = mergeCmd.call();
        if (result.getMergeStatus().isSuccessful())
        {
            return new GitCommandResult(0, "Merge made by the 'recursive' strategy.\n", "");
        }
        else
        {
            return new GitCommandResult(1, "", "Merge failed: " + result.getMergeStatus());
        }
    }
}
