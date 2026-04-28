/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.MergeCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.AmbiguousObjectException;
import org.eclipse.jgit.errors.IncorrectObjectTypeException;
import org.eclipse.jgit.lib.RepositoryState;
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
            .addParameter("<commit>", "Branch or commit to merge into current branch")
            .addParameter("--ff", "Allow fast-forward (default)")
            .addParameter("--no-ff", "Always create a merge commit, even if fast-forward is possible")
            .addParameter("--ff-only", "Refuse to merge unless fast-forward is possible")
            .addParameter("--squash", "Produce squashed working-tree changes (no commit, no merge)")
            .addParameter("--abort", "Abort an in-progress merge and restore pre-merge state")
            .addParameter("-s, --strategy <name>", "Merge strategy: recursive | resolve | ours | theirs | simple-two-way-in-core")
            .addParameter("-m <msg>", "Commit message for the merge");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args)
        throws GitAPIException, AmbiguousObjectException, IncorrectObjectTypeException, IOException
    {
        if (args.contains("--abort"))
        {
            return handleAbort(git);
        }

        String branch = null;
        String message = null;
        MergeStrategy strategy = MergeStrategy.RECURSIVE;
        MergeCommand.FastForwardMode ff = null;
        var squash = false;

        for (int i = 0; i < args.size(); i++)
        {
            var arg = args.get(i);
            if (arg.equals("--ff"))
            {
                ff = MergeCommand.FastForwardMode.FF;
            }
            else if (arg.equals("--no-ff"))
            {
                ff = MergeCommand.FastForwardMode.NO_FF;
            }
            else if (arg.equals("--ff-only"))
            {
                ff = MergeCommand.FastForwardMode.FF_ONLY;
            }
            else if (arg.equals("--squash"))
            {
                squash = true;
            }
            else if ((arg.equals("-s") || arg.equals("--strategy")) && i + 1 < args.size())
            {
                strategy = parseStrategy(args.get(++i));
                if (strategy == null)
                {
                    return new GitCommandResult(1, "", "fatal: unknown merge strategy: " + args.get(i));
                }
            }
            else if (arg.startsWith("--strategy="))
            {
                strategy = parseStrategy(arg.substring("--strategy=".length()));
                if (strategy == null)
                {
                    return new GitCommandResult(1, "", "fatal: unknown merge strategy: " + arg);
                }
            }
            else if (arg.equals("-m") && i + 1 < args.size())
            {
                message = args.get(++i);
            }
            else if (!arg.startsWith("-") && branch == null)
            {
                branch = arg;
            }
        }

        if (branch == null)
        {
            return new GitCommandResult(1, "", "fatal: you must specify a branch to merge");
        }

        var ref = git.getRepository().resolve(branch);
        if (ref == null)
        {
            return new GitCommandResult(1, "", "fatal: " + branch + " - not found");
        }

        var mergeCmd = git.merge();
        mergeCmd.include(ref);
        mergeCmd.setStrategy(strategy);
        if (ff != null)
        {
            mergeCmd.setFastForward(ff);
        }
        if (squash)
        {
            mergeCmd.setSquash(true);
        }
        if (message != null)
        {
            mergeCmd.setMessage(message);
        }

        var result = mergeCmd.call();
        var status = result.getMergeStatus();
        
        // Check for conflicts by examining repository state
        var repoState = git.getRepository().getRepositoryState();
        if (repoState == RepositoryState.MERGING)
        {
            return new GitCommandResult(1, "", 
                "Automatic merge failed; fix conflicts and then commit the result.\n" +
                "You can use the " + EditMcpTool.TOOL_NAME + " tool to resolve conflicts in conflicting files.");
        }
        
        if (status.isSuccessful())
        {
            return new GitCommandResult(0, "Merge result: " + status + "\n", "");
        }
        return new GitCommandResult(1, "", "Merge failed: " + status);
    }

    @SuppressWarnings("nls")
    private GitCommandResult handleAbort(Git git) throws GitAPIException, IOException
    {
        var state = git.getRepository().getRepositoryState();
        if (state != RepositoryState.MERGING && state != RepositoryState.MERGING_RESOLVED)
        {
            return new GitCommandResult(1, "", "fatal: There is no merge to abort");
        }
        // Reset working tree and index to HEAD to abort the merge.
        git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).setRef("HEAD").call();
        return new GitCommandResult(0, "Merge aborted.\n", "");
    }

    @SuppressWarnings("nls")
    private static MergeStrategy parseStrategy(String name)
    {
        if (name == null)
        {
            return null;
        }
        switch (name)
        {
            case "recursive":
                return MergeStrategy.RECURSIVE;
            case "resolve":
                return MergeStrategy.RESOLVE;
            case "ours":
                return MergeStrategy.OURS;
            case "theirs":
                return MergeStrategy.THEIRS;
            case "simple":
            case "simple-two-way-in-core":
                return MergeStrategy.SIMPLE_TWO_WAY_IN_CORE;
            default:
                return null;
        }
    }
}
