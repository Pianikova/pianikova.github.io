/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.List;

import org.eclipse.jgit.api.Git;

/**
 * Git stash command implementation
 */
public class JGitStash implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "stash"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Stash the changes in a dirty working directory away")
            .addParameter("(no arguments)", "Stash current changes")
            .addParameter("list", "List stashed changes")
            .addParameter("apply [<stash>]", "Apply stashed changes")
            .addParameter("pop [<stash>]", "Apply and remove stashed changes")
            .addParameter("drop [<stash>]", "Remove stashed changes")
            .addParameter("<stash>", "Stash reference (e.g., stash@{0})");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args)
    {
        if (args.isEmpty())
        {
            try
            {
                var stashCommit = git.stashCreate().call();
                if (stashCommit == null)
                {
                    return new GitCommandResult(0, "No local changes to save\n", "");
                }
                return new GitCommandResult(0, "Saved working directory and index state " + stashCommit.getName() + "\n", "");
            }
            catch (Exception e)
            {
                var errorMsg = e.getMessage();
                if (errorMsg != null && errorMsg.contains("No changes"))
                {
                    return new GitCommandResult(0, "No local changes to save\n", "");
                }
                if (errorMsg == null || errorMsg.isEmpty())
                {
                    errorMsg = "Failed to stash";
                }
                return new GitCommandResult(1, "", "error: " + errorMsg + "\n");
            }
        }

        var subCommand = args.get(0);
        switch (subCommand)
        {
            case "list":
                try
                {
                    var stashes = git.stashList().call();
                    var sb = new StringBuilder();
                    var index = 0;
                    for (var stash : stashes)
                    {
                        sb.append("stash@{").append(index++).append("}: ")
                            .append(stash.getShortMessage()).append("\n");
                    }
                    return new GitCommandResult(0, sb.toString(), "");
                }
                catch (Exception e)
                {
                    var errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty())
                    {
                        errorMsg = "Failed to list stashes";
                    }
                    return new GitCommandResult(1, "", "error: " + errorMsg + "\n");
                }
            case "apply":
                try
                {
                    var applyIndex = 0;
                    if (args.size() > 1)
                    {
                        applyIndex = extractStashIndex(args.get(1));
                    }
                    git.stashApply().setStashRef("stash@{" + applyIndex + "}").call();
                    return new GitCommandResult(0, "", "");
                }
                catch (Exception e)
                {
                    var errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty())
                    {
                        errorMsg = "Failed to apply stash";
                    }
                    return new GitCommandResult(1, "", "error: " + errorMsg + "\n");
                }
            case "drop":
                try
                {
                    var dropIndex = 0;
                    if (args.size() > 1)
                    {
                        dropIndex = extractStashIndex(args.get(1));
                    }
                    git.stashDrop().setStashRef(dropIndex).call();
                    return new GitCommandResult(0, "", "");
                }
                catch (Exception e)
                {
                    var errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty())
                    {
                        errorMsg = "Failed to drop stash";
                    }
                    return new GitCommandResult(1, "", "error: " + errorMsg + "\n");
                }
            case "pop":
                try
                {
                    var popIndex = 0;
                    if (args.size() > 1)
                    {
                        popIndex = extractStashIndex(args.get(1));
                    }
                    git.stashApply().setStashRef("stash@{" + popIndex + "}").call();
                    git.stashDrop().setStashRef(popIndex).call();
                    return new GitCommandResult(0, "", "");
                }
                catch (Exception e)
                {
                    var errorMsg = e.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty())
                    {
                        errorMsg = "Failed to pop stash";
                    }
                    return new GitCommandResult(1, "", "error: " + errorMsg + "\n");
                }
            default:
                return new GitCommandResult(1, "", "error: unknown stash command: " + subCommand + "\n");
        }
    }

    @SuppressWarnings("nls")
    private int extractStashIndex(String stashRef)
    {
        if (stashRef.startsWith("stash@{"))
        {
            var end = stashRef.indexOf('}');
            if (end > 0)
            {
                try
                {
                    return Integer.parseInt(stashRef.substring("stash@{".length(), end));
                }
                catch (NumberFormatException e)
                {
                    // ignore
                }
            }
        }
        return 0;
    }
}
