/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.ListBranchCommand;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.revwalk.RevWalk;

import com.google.inject.Inject;

/**
 * Git branch command implementation
 */
public class JGitBranch implements IJGitCommand
{
    private final IJGitCommonHelper commonHelper;

    @Inject
    public JGitBranch(IJGitCommonHelper commonHelper)
    {
        this.commonHelper = commonHelper;
    }

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
            .addParameter("<branchname>", "Create branch (no list flags) or filter listing")
            .addParameter("-d <branchname>", "Delete a fully merged branch")
            .addParameter("-D <branchname>", "Force delete a branch")
            .addParameter("-a, --all", "Show both local and remote branches")
            .addParameter("-r, --remotes", "Show remote-tracking branches")
            .addParameter("-u <upstream>, --set-upstream-to=<upstream>",
                "Set upstream for the current (or named) branch")
            .addParameter("--merged [<commit>]", "List branches merged into the given commit (default: HEAD)")
            .addParameter("--no-merged [<commit>]", "List branches not merged into the given commit")
            .addParameter("-vv", "List branches with tracking info (ahead/behind)");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        git.getRepository().getRefDatabase().refresh();
        ListBranchCommand.ListMode listMode = null;
        var deleteBranch = "";
        var forceDelete = false;
        var createBranch = "";
        var startPoint = "HEAD";
        String setUpstreamTo = null;
        var verbose2 = false;
        Boolean merged = null; // null = not specified, true = --merged, false = --no-merged
        String mergedRef = "HEAD";

        for (int i = 0; i < args.size(); i++)
        {
            var arg = args.get(i);
            if (arg.equals("-a") || arg.equals("--all"))
            {
                listMode = ListBranchCommand.ListMode.ALL;
            }
            else if (arg.equals("-r") || arg.equals("--remotes"))
            {
                listMode = ListBranchCommand.ListMode.REMOTE;
            }
            else if (arg.equals("-d"))
            {
                if (i + 1 < args.size())
                {
                    deleteBranch = args.get(++i);
                }
            }
            else if (arg.equals("-D"))
            {
                forceDelete = true;
                if (i + 1 < args.size())
                {
                    deleteBranch = args.get(++i);
                }
            }
            else if (arg.equals("-u") || arg.equals("--set-upstream-to"))
            {
                if (i + 1 < args.size())
                {
                    setUpstreamTo = args.get(++i);
                }
            }
            else if (arg.startsWith("--set-upstream-to="))
            {
                setUpstreamTo = arg.substring("--set-upstream-to=".length());
            }
            else if (arg.equals("-vv") || arg.equals("--verbose-tracking"))
            {
                verbose2 = true;
            }
            else if (arg.equals("--merged"))
            {
                merged = Boolean.TRUE;
                if (i + 1 < args.size() && !args.get(i + 1).startsWith("-"))
                {
                    mergedRef = args.get(++i);
                }
            }
            else if (arg.equals("--no-merged"))
            {
                merged = Boolean.FALSE;
                if (i + 1 < args.size() && !args.get(i + 1).startsWith("-"))
                {
                    mergedRef = args.get(++i);
                }
            }
            else if (!arg.startsWith("-"))
            {
                if (createBranch.isEmpty())
                {
                    createBranch = arg;
                }
                else if (startPoint.equals("HEAD"))
                {
                    startPoint = arg;
                }
            }
        }

        if (setUpstreamTo != null)
        {
            return setUpstream(git, createBranch, setUpstreamTo);
        }

        if (!deleteBranch.isEmpty())
        {
            git.branchDelete().setBranchNames(deleteBranch).setForce(forceDelete).call();
            git.getRepository().getRefDatabase().refresh();
            git.getRepository().getRefDatabase().refresh();
            return new GitCommandResult(0, "Deleted branch " + deleteBranch + "\n", "");
        }

        // Create only if name is set AND no list-only flag, AND no merged filter.
        if (!createBranch.isEmpty() && listMode == null && merged == null)
        {
            var branchCmd = git.branchCreate();
            branchCmd.setName(createBranch);
            branchCmd.setStartPoint(startPoint);
            branchCmd.call();
            git.getRepository().getRefDatabase().refresh();
            git.getRepository().getRefDatabase().refresh();
            return new GitCommandResult(0, "", "");
        }

        return listBranches(git, listMode, verbose2, merged, mergedRef);
    }

    @SuppressWarnings("nls")
    private GitCommandResult setUpstream(Git git, String branchName, String upstream) throws IOException
    {
        var repository = git.getRepository();
        var local = (branchName == null || branchName.isEmpty()) ? repository.getBranch() : branchName;
        if (local == null || local.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: no branch to configure");
        }
        var remote = "origin";
        var remoteBranch = upstream;
        var slash = upstream.indexOf('/');
        if (slash > 0)
        {
            remote = upstream.substring(0, slash);
            remoteBranch = upstream.substring(slash + 1);
        }
        var config = repository.getConfig();
        config.setString("branch", local, "remote", remote);
        config.setString("branch", local, "merge", Constants.R_HEADS + remoteBranch);
        config.save();
        return new GitCommandResult(0,
            "Branch '" + local + "' set up to track '" + remote + "/" + remoteBranch + "'.\n", "");
    }

    @SuppressWarnings("nls")
    private GitCommandResult listBranches(Git git, ListBranchCommand.ListMode listMode, boolean verbose2,
        Boolean merged, String mergedRef) throws GitAPIException, IOException
    {
        var repository = git.getRepository();
        repository.getRefDatabase().refresh();
        var currentBranch = repository.getBranch();
        var branches = new ArrayList<Ref>();
        var refDatabase = repository.getRefDatabase();

        var allRefs = refDatabase.getRefs();
        for (var ref : allRefs)
        {
            var name = ref.getName();
            if (listMode == ListBranchCommand.ListMode.ALL)
            {
                if (name.startsWith(Constants.R_HEADS) || name.startsWith(Constants.R_REMOTES))
                {
                    branches.add(ref);
                }
            }
            else if (listMode == ListBranchCommand.ListMode.REMOTE)
            {
                if (name.startsWith(Constants.R_REMOTES))
                {
                    branches.add(ref);
                }
            }
            else
            {
                if (name.startsWith(Constants.R_HEADS))
                {
                    branches.add(ref);
                }
            }
        }

        if (merged != null)
        {
            var refId = repository.resolve(mergedRef);
            if (refId == null)
            {
                return new GitCommandResult(1, "", "fatal: cannot resolve '" + mergedRef + "'");
            }
            var filtered = new ArrayList<Ref>();
            try (var revWalk = new RevWalk(repository))
            {
                var target = revWalk.parseCommit(refId);
                for (var ref : branches)
                {
                    var id = ref.getObjectId();
                    if (id == null)
                    {
                        continue;
                    }
                    try
                    {
                        var c = revWalk.parseCommit(id);
                        var isMerged = revWalk.isMergedInto(c, target);
                        if ((merged && isMerged) || (!merged && !isMerged))
                        {
                            filtered.add(ref);
                        }
                    }
                    catch (Exception ignored)
                    {
                        // skip unresolvable
                    }
                }
            }
            branches = filtered;
        }

        var sb = new StringBuilder();
        for (var ref : branches)
        {
            var name = ref.getName();
            var isRemote = name.startsWith(Constants.R_REMOTES);
            var isLocal = name.startsWith(Constants.R_HEADS);
            if (isLocal)
            {
                name = name.substring(Constants.R_HEADS.length());
            }
            else if (isRemote)
            {
                name = name.substring(Constants.R_REMOTES.length());
            }

            sb.append(name.equals(currentBranch) ? "* " : "  ");
            sb.append(name);

            if (verbose2 && isLocal)
            {
                var id = ref.getObjectId();
                if (id != null)
                {
                    sb.append(" ").append(id.abbreviate(7).name());
                    try (var revWalk = new RevWalk(repository))
                    {
                        sb.append(" ").append(revWalk.parseCommit(id).getShortMessage());
                    }
                    catch (Exception ignored)
                    {
                        // keep branch listing usable even when a ref cannot be parsed
                    }
                }
                var status = commonHelper.getBranchTrackingStatus(git, name);
                if (status != null)
                {
                    sb.append(" [").append(getTrackingBranchName(repository, name));
                    if (status.getAheadCount() > 0)
                    {
                        sb.append(": ahead ").append(status.getAheadCount());
                    }
                    if (status.getBehindCount() > 0)
                    {
                        sb.append(status.getAheadCount() > 0 ? ", " : ": ");
                        sb.append("behind ").append(status.getBehindCount());
                    }
                    sb.append("]");
                }
            }
            sb.append("\n");
        }

        return new GitCommandResult(0, sb.toString(), "");
    }

    @SuppressWarnings("nls")
    private static String getTrackingBranchName(org.eclipse.jgit.lib.Repository repo, String localBranch)
    {
        var config = repo.getConfig();
        var remote = config.getString("branch", localBranch, "remote");
        var merge = config.getString("branch", localBranch, "merge");
        if (remote == null || merge == null)
        {
            return "";
        }
        var name = merge.startsWith(Constants.R_HEADS) ? merge.substring(Constants.R_HEADS.length()) : merge;
        return remote + "/" + name;
    }
}
