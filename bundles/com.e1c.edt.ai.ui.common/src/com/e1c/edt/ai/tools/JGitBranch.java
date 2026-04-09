/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;

/**
 * Git branch command implementation
 */
public class JGitBranch implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "branch"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("List, create, or delete branches")
            .addParameter("<branchname>", "Create or show branch")
            .addParameter("-d <branchname>", "Delete a branch")
            .addParameter("-a", "Show both local and remote branches")
            .addParameter("-r", "Show remote-tracking branches");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        ListBranchCommand.ListMode listMode = null;
        var showCurrent = false;
        var deleteBranch = "";
        var createBranch = "";
        var startPoint = "HEAD";

        for (var arg : args)
        {
            if (arg.equals("-a") || arg.equals("--all"))
            {
                listMode = ListBranchCommand.ListMode.ALL;
                showCurrent = true;
            }
            else if (arg.equals("-r") || arg.equals("--remotes"))
            {
                listMode = ListBranchCommand.ListMode.REMOTE;
                showCurrent = true;
            }
            else if (arg.equals("-d"))
            {
                listMode = null;
            }
            else if (!arg.startsWith("-"))
            {
                if (listMode == null && args.contains("-d"))
                {
                    if (deleteBranch.isEmpty())
                    {
                        deleteBranch = arg;
                    }
                }
                else if (listMode == null)
                {
                    if (createBranch.isEmpty())
                    {
                        createBranch = arg;
                    }
                }
                else
                {
                    if (createBranch.isEmpty())
                    {
                        createBranch = arg;
                    }
                }
            }
        }

        if (!createBranch.isEmpty())
        {
            var branchCmd = git.branchCreate();
            branchCmd.setName(createBranch);
            branchCmd.setStartPoint(startPoint);
            branchCmd.call();
            return new GitCommandResult(0, "", "");
        }

        if (!deleteBranch.isEmpty())
        {
            git.branchDelete().setBranchNames(deleteBranch).setForce(false).call();
            return new GitCommandResult(0, "", "");
        }

        if (showCurrent || args.isEmpty())
        {
            var currentBranch = git.getRepository().getBranch();
            var sb = new StringBuilder();
            
            if (listMode == ListBranchCommand.ListMode.ALL)
            {
                var localBranches = git.branchList().call();
                var remoteBranches = git.branchList().setListMode(ListBranchCommand.ListMode.REMOTE).call();
                
                for (Ref branch : localBranches)
                {
                    var name = branch.getName();
                    if (!name.startsWith(Constants.R_HEADS))
                    {
                        continue;
                    }
                    name = name.substring(Constants.R_HEADS.length());
                    if (name.equals(currentBranch))
                    {
                        sb.append("* ");
                    }
                    else
                    {
                        sb.append("  ");
                    }
                    sb.append(name).append("\n");
                }
                
                for (Ref branch : remoteBranches)
                {
                    var name = branch.getName().substring(Constants.R_REMOTES.length());
                    sb.append("  ").append(name).append("\n");
                }
            }
            else
            {
                var branchListCmd = git.branchList();
                if (listMode != null && listMode != ListBranchCommand.ListMode.ALL)
                {
                    branchListCmd.setListMode(listMode);
                }
                var branches = branchListCmd.call();
                
                for (Ref branch : branches)
                {
                    var name = branch.getName();
                    var isRemote = name.startsWith(Constants.R_REMOTES);
                    var isLocal = name.startsWith(Constants.R_HEADS);
                    
                    if (listMode == ListBranchCommand.ListMode.REMOTE && !isRemote)
                    {
                        continue;
                    }
                    
                    if ((listMode == null || listMode != ListBranchCommand.ListMode.REMOTE) && !isLocal)
                    {
                        continue;
                    }
                    
                    if (isLocal)
                    {
                        name = name.substring(Constants.R_HEADS.length());
                    }
                    else if (isRemote)
                    {
                        name = name.substring(Constants.R_REMOTES.length());
                    }
                    
                    if (name.equals(currentBranch))
                    {
                        sb.append("* ");
                    }
                    else
                    {
                        sb.append("  ");
                    }
                    sb.append(name).append("\n");
                }
            }
            
            return new GitCommandResult(0, sb.toString(), "");
        }

        return new GitCommandResult(0, "", "");
    }
}
