/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.Repository;

/**
 * Git rev-parse command implementation.
 * Supports the most common revision queries used by tooling.
 */
public class JGitRevParse implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "rev-parse"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Pick out and massage parameters")
            .addParameter("<rev>", "Revision expression to resolve (e.g. HEAD, HEAD~3, branch@{u}, abbrev-SHA)")
            .addParameter("--abbrev-ref", "Print the symbolic name (branch ref) instead of the SHA")
            .addParameter("--short[=<n>]", "Output an abbreviated SHA (default 7 chars)")
            .addParameter("--show-toplevel", "Show the absolute path of the top-level working directory")
            .addParameter("--git-dir", "Show the path to the .git directory")
            .addParameter("--is-inside-work-tree", "Print 'true' if inside a working tree")
            .addParameter("--verify", "Resolve and fail if the revision cannot be parsed");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws Exception
    {
        var repository = git.getRepository();
        var abbrevRef = false;
        var shortOut = false;
        var shortLen = 7;
        var verify = false;
        var revs = new ArrayList<String>();

        for (var arg : args)
        {
            if (arg.equals("--show-toplevel"))
            {
                var workTree = repository.getWorkTree();
                return new GitCommandResult(0, workTree.getAbsolutePath() + "\n", "");
            }
            if (arg.equals("--git-dir"))
            {
                return new GitCommandResult(0, repository.getDirectory().getAbsolutePath() + "\n", "");
            }
            if (arg.equals("--is-inside-work-tree"))
            {
                return new GitCommandResult(0, (repository.isBare() ? "false" : "true") + "\n", "");
            }
            if (arg.equals("--abbrev-ref"))
            {
                abbrevRef = true;
            }
            else if (arg.equals("--short"))
            {
                shortOut = true;
            }
            else if (arg.startsWith("--short="))
            {
                shortOut = true;
                try
                {
                    shortLen = Integer.parseInt(arg.substring("--short=".length()));
                }
                catch (NumberFormatException ignored)
                {
                    // keep default
                }
            }
            else if (arg.equals("--verify"))
            {
                verify = true;
            }
            else if (!arg.startsWith("-"))
            {
                revs.add(arg);
            }
        }

        if (revs.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: no revision specified");
        }

        var sb = new StringBuilder();
        for (var rev : revs)
        {
            if (abbrevRef)
            {
                var shortName = resolveSymbolic(repository, rev);
                if (shortName == null)
                {
                    if (verify)
                    {
                        return new GitCommandResult(128, "", "fatal: bad revision '" + rev + "'");
                    }
                    sb.append(rev).append("\n");
                }
                else
                {
                    sb.append(shortName).append("\n");
                }
                continue;
            }
            var id = repository.resolve(rev);
            if (id == null)
            {
                if (verify)
                {
                    return new GitCommandResult(128, "", "fatal: bad revision '" + rev + "'");
                }
                sb.append(rev).append("\n");
                continue;
            }
            sb.append(shortOut ? id.abbreviate(shortLen).name() : id.getName()).append("\n");
        }
        return new GitCommandResult(0, sb.toString(), "");
    }

    private static String resolveSymbolic(Repository repository, String rev) throws java.io.IOException
    {
        if ("HEAD".equals(rev))
        {
            var head = repository.getFullBranch();
            if (head == null)
            {
                return null;
            }
            if (head.startsWith(Constants.R_HEADS))
            {
                return head.substring(Constants.R_HEADS.length());
            }
            return head;
        }
        var ref = repository.findRef(rev);
        if (ref == null)
        {
            return null;
        }
        var name = ref.getName();
        if (name.startsWith(Constants.R_HEADS))
        {
            return name.substring(Constants.R_HEADS.length());
        }
        if (name.startsWith(Constants.R_REMOTES))
        {
            return name.substring(Constants.R_REMOTES.length());
        }
        if (name.startsWith(Constants.R_TAGS))
        {
            return name.substring(Constants.R_TAGS.length());
        }
        return name;
    }
}
