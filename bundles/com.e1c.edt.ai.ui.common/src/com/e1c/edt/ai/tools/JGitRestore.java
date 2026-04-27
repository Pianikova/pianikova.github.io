/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.CheckoutCommand.Stage;
import org.eclipse.jgit.api.Git;

/**
 * Modern equivalent of `git checkout -- <paths>`: restores files from the index or a source.
 */
public class JGitRestore implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "restore"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Restore working tree files (modern alternative to checkout for files)")
            .addParameter("<pathspec>...", "Files to restore")
            .addParameter("--source=<rev>, -s <rev>", "Restore from the given revision (default: index)")
            .addParameter("--staged", "Restore the staged content from HEAD (unstage changes)")
            .addParameter("--worktree", "Restore in the working tree (default)")
            .addParameter("--theirs", "When conflicting, take 'theirs' content")
            .addParameter("--ours", "When conflicting, take 'ours' content");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws Exception
    {
        String source = null;
        var staged = false;
        var worktree = false;
        Stage stage = null;
        var paths = new ArrayList<String>();
        var afterDoubleDash = false;

        for (int i = 0; i < args.size(); i++)
        {
            var arg = args.get(i);
            if (afterDoubleDash)
            {
                paths.add(arg);
                continue;
            }
            if (arg.equals("--"))
            {
                afterDoubleDash = true;
            }
            else if (arg.equals("--staged") || arg.equals("--cached"))
            {
                staged = true;
            }
            else if (arg.equals("--worktree"))
            {
                worktree = true;
            }
            else if (arg.equals("--theirs"))
            {
                stage = Stage.THEIRS;
            }
            else if (arg.equals("--ours"))
            {
                stage = Stage.OURS;
            }
            else if (arg.equals("-s") || arg.equals("--source"))
            {
                if (i + 1 < args.size())
                {
                    source = args.get(++i);
                }
            }
            else if (arg.startsWith("--source="))
            {
                source = arg.substring("--source=".length());
            }
            else if (!arg.startsWith("-"))
            {
                paths.add(arg);
            }
        }

        if (paths.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: you must specify path(s) to restore");
        }

        if (staged && !worktree)
        {
            // Unstage: reset the path back to HEAD
            var resetCmd = git.reset();
            resetCmd.setRef("HEAD");
            for (var p : paths)
            {
                resetCmd.addPath(p);
            }
            resetCmd.call();
            return new GitCommandResult(0, "", "");
        }

        var checkoutCmd = git.checkout();
        for (var p : paths)
        {
            checkoutCmd.addPath(p);
        }
        if (source != null)
        {
            checkoutCmd.setStartPoint(source);
        }
        if (stage != null)
        {
            checkoutCmd.setStage(stage);
        }
        checkoutCmd.call();
        return new GitCommandResult(0, "", "");
    }
}
