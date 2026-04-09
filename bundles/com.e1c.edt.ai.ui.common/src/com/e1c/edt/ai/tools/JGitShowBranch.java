/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.revwalk.RevWalk;

/**
 * Git show-branch command implementation
 */
public class JGitShowBranch implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "show-branch"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Show branches and their commits")
            .addParameter("<branch>...", "Branches to show (default: all branches)");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        var repository = git.getRepository();

        // Get all branches
        var branches = git.branchList().call();
        var branchNames = new ArrayList<String>();
        var branchCommits = new HashMap<String, ObjectId>();

        for (var branch : branches)
        {
            var name = branch.getName();
            if (name.startsWith(Constants.R_HEADS))
            {
                name = name.substring(Constants.R_HEADS.length());
                branchNames.add(name);
                branchCommits.put(name, branch.getObjectId());
            }
        }

        // If specific branches are requested, filter them
        if (!args.isEmpty())
        {
            var requested = new ArrayList<String>();
            for (var arg : args)
            {
                if (!arg.startsWith("-"))
                {
                    requested.add(arg);
                }
            }
            if (!requested.isEmpty())
            {
                branchNames.retainAll(requested);
            }
        }

        if (branchNames.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: no branches specified or found");
        }

        var sb = new StringBuilder();

        // Show branch headers
        for (var name : branchNames)
        {
            sb.append("[").append(name).append("] ");
        }
        sb.append("\n");

        try (// Get recent commits from each branch
            var revWalk = new RevWalk(repository))
        {
            var commitMessages = new HashMap<String, List<String>>();

            for (var name : branchNames)
            {
                var commitId = branchCommits.get(name);
                if (commitId != null)
                {
                    var commit = revWalk.parseCommit(commitId);
                    var messages = new ArrayList<String>();

                    // Get up to 10 commits from this branch
                    try (var branchWalk = new RevWalk(repository))
                    {
                        branchWalk.markStart(commit);
                        int count = 0;
                        for (var c : branchWalk)
                        {
                            if (count++ >= 10)
                            {
                                break;
                            }
                            messages.add(c.getShortMessage());
                        }
                    }

                    commitMessages.put(name, messages);
                }
            }

            // Show commits
            var maxCommits = commitMessages.values().stream().mapToInt(List::size).max().orElse(0);

            var maxNameLength = branchNames.stream().mapToInt(String::length).max().orElse(0);
            var prefixLength = maxNameLength + 3;

            for (int i = 0; i < maxCommits; i++)
            {
                for (var name : branchNames)
                {
                    var messages = commitMessages.get(name);
                    if (messages != null && i < messages.size())
                    {
                        sb.append("[").append(name).append("] ").append(messages.get(i));
                    }
                    else
                    {
                        var padding = String.format("%" + prefixLength + "s", "");
                        sb.append(padding);
                    }
                    sb.append("   ");
                }
                sb.append("\n");
            }
        }
        return new GitCommandResult(0, sb.toString(), "");
    }
}
