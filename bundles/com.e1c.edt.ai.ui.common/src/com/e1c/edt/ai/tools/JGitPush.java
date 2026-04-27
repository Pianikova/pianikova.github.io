/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.transport.RefSpec;

/**
 * Git push command implementation
 */
public class JGitPush implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "push"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Update remote refs along with associated objects")
            .addParameter("<remote>", "Remote repository name (default: origin)")
            .addParameter("<refspec>", "Optional source[:dest] branch")
            .addParameter("--force, -f", "Force updates even if not fast-forward")
            .addParameter("--all", "Push all branches")
            .addParameter("-u, --set-upstream", "Set upstream tracking for the pushed branch")
            .addParameter("--delete, -d", "Delete the named ref on the remote")
            .addParameter("--force-with-lease=<ref>:<expected>",
                "Force push only if remote ref still matches <expected> sha");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        var remote = "origin";
        String branchSpec = null;
        var force = false;
        var all = false;
        var setUpstream = false;
        var delete = false;
        String forceWithLease = null;
        var forceWithLeaseRequested = false;

        var nonOptionIndex = 0;
        for (var arg : args)
        {
            if (arg.equals("-f") || arg.equals("--force"))
            {
                force = true;
            }
            else if (arg.equals("--all"))
            {
                all = true;
            }
            else if (arg.equals("-u") || arg.equals("--set-upstream"))
            {
                setUpstream = true;
            }
            else if (arg.equals("-d") || arg.equals("--delete"))
            {
                delete = true;
            }
            else if (arg.equals("--force-with-lease"))
            {
                forceWithLeaseRequested = true;
            }
            else if (arg.startsWith("--force-with-lease="))
            {
                forceWithLeaseRequested = true;
                forceWithLease = arg.substring("--force-with-lease=".length());
            }
            else if (!arg.startsWith("-"))
            {
                if (nonOptionIndex == 0)
                {
                    remote = arg;
                }
                else if (nonOptionIndex == 1)
                {
                    branchSpec = arg;
                }
                nonOptionIndex++;
            }
        }

        if (forceWithLeaseRequested)
        {
            if (forceWithLease == null || forceWithLease.isBlank())
            {
                return new GitCommandResult(1, "",
                    "fatal: --force-with-lease requires <ref>:<expected> argument");
            }
            var colon = forceWithLease.indexOf(':');
            if (colon <= 0 || colon == forceWithLease.length() - 1)
            {
                return new GitCommandResult(1, "",
                    "fatal: --force-with-lease expects <ref>:<expected_sha> form");
            }
            var leaseRef = forceWithLease.substring(0, colon);
            var leaseExpected = forceWithLease.substring(colon + 1);
            var fullRef = leaseRef.startsWith(Constants.R_REFS) ? leaseRef : Constants.R_HEADS + leaseRef;

            var remoteRefs = git.lsRemote().setRemote(remote).setHeads(true).setTags(true).call();
            String actual = null;
            for (var ref : remoteRefs)
            {
                if (ref.getName().equals(fullRef))
                {
                    actual = ref.getObjectId() != null ? ref.getObjectId().getName() : null;
                    break;
                }
            }
            if (actual == null)
            {
                return new GitCommandResult(1, "",
                    "fatal: remote ref " + fullRef + " not found on " + remote);
            }
            if (!actual.startsWith(leaseExpected) && !leaseExpected.startsWith(actual))
            {
                return new GitCommandResult(1, "",
                    "stale info: expected " + leaseExpected + " but remote has " + actual);
            }
            force = true;
        }

        var pushCmd = git.push();
        var currentBranch = git.getRepository().getBranch();

        if (delete)
        {
            if (branchSpec == null || branchSpec.isBlank())
            {
                return new GitCommandResult(1, "", "fatal: --delete requires a ref name");
            }
            var refName = branchSpec.startsWith(Constants.R_HEADS) ? branchSpec
                : Constants.R_HEADS + branchSpec;
            pushCmd.setRefSpecs(new RefSpec(":" + refName));
        }
        else if (branchSpec != null)
        {
            var refSpecString = branchSpec.contains(":") ? branchSpec : branchSpec + ":" + branchSpec;
            pushCmd.setRefSpecs(new RefSpec(refSpecString));
        }
        pushCmd.setRemote(remote);
        if (force)
        {
            pushCmd.setForce(true);
        }
        if (all)
        {
            pushCmd.setPushAll();
        }

        var results = pushCmd.call();
        var message = new StringBuilder();
        message.append("To ").append(remote).append("\n");
        for (var result : results)
        {
            for (var update : result.getRemoteUpdates())
            {
                var newId = update.getNewObjectId();
                if (newId != null)
                {
                    message.append(update.getRemoteName()).append(" -> ").append(newId.abbreviate(7)).append("\n");
                }
            }
        }

        if (setUpstream && !delete && currentBranch != null && !currentBranch.isEmpty())
        {
            var localBranch = currentBranch;
            var remoteBranch = currentBranch;
            if (branchSpec != null)
            {
                var src = branchSpec.contains(":") ? branchSpec.substring(0, branchSpec.indexOf(':'))
                    : branchSpec;
                var dst = branchSpec.contains(":") ? branchSpec.substring(branchSpec.indexOf(':') + 1)
                    : branchSpec;
                if (src.startsWith(Constants.R_HEADS))
                {
                    src = src.substring(Constants.R_HEADS.length());
                }
                if (dst.startsWith(Constants.R_HEADS))
                {
                    dst = dst.substring(Constants.R_HEADS.length());
                }
                if (!src.isEmpty())
                {
                    localBranch = src;
                }
                if (!dst.isEmpty())
                {
                    remoteBranch = dst;
                }
            }
            var config = git.getRepository().getConfig();
            config.setString("branch", localBranch, "remote", remote);
            config.setString("branch", localBranch, "merge", Constants.R_HEADS + remoteBranch);
            config.save();
            message.append("Branch '").append(localBranch).append("' set up to track '")
                .append(remote).append("/").append(remoteBranch).append("'.\n");
        }

        return new GitCommandResult(0, message.toString(), "");
    }
}
