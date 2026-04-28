/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.PersonIdent;

/**
 * Git commit command implementation
 */
public class JGitCommit implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "commit"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Record changes to the repository")
            .addParameter("-m <message>", "Use the given <message> as the commit message")
            .addParameter("-a", "Stage modified and deleted files, then commit")
            .addParameter("--amend", "Amend the previous commit")
            .addParameter("--allow-empty", "Allow creating a commit with no changes")
            .addParameter("--no-verify", "Bypass pre-commit and commit-msg hooks")
            .addParameter("--author <author>", "Override the commit author. Format: \"Name <email>\"");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        var commitCmd = git.commit();
        String message = null;
        var allowEmpty = false;
        var missingMessageValue = false;

        for (int i = 0; i < args.size(); i++)
        {
            var arg = args.get(i);
            if (arg.equals("-m"))
            {
                if (i + 1 < args.size())
                {
                    message = args.get(i + 1);
                    i++;
                }
                else
                {
                    missingMessageValue = true;
                }
            }
            else if (arg.equals("-a"))
            {
                commitCmd.setAll(true);
            }
            else if (arg.equals("--amend"))
            {
                commitCmd.setAmend(true);
            }
            else if (arg.equals("--allow-empty"))
            {
                allowEmpty = true;
            }
            else if (arg.equals("--no-verify"))
            {
                // JGit does not run hooks by default; flag accepted for CLI compatibility.
            }
            else if (arg.equals("--author"))
            {
                if (i + 1 < args.size())
                {
                    var author = parseAuthor(args.get(i + 1));
                    if (author != null)
                    {
                        commitCmd.setAuthor(author);
                    }
                    i++;
                }
            }
            else if (arg.startsWith("--author="))
            {
                var author = parseAuthor(arg.substring("--author=".length()));
                if (author != null)
                {
                    commitCmd.setAuthor(author);
                }
            }
        }

        commitCmd.setAllowEmpty(allowEmpty);

        if (missingMessageValue)
        {
            return new GitCommandResult(1, "", "error: switch `m' requires a value\n");
        }
        if (message == null)
        {
            return new GitCommandResult(1, "", "fatal: no commit message specified\n");
        }

        commitCmd.setMessage(message);

        var commit = commitCmd.call();
        return new GitCommandResult(0, "[" + git.getRepository().getBranch() + " "
            + commit.getName().substring(0, 7) + "] " + commitCmd.getMessage() + "\n", "");
    }

    @SuppressWarnings("nls")
    private static PersonIdent parseAuthor(String author)
    {
        if (author == null || author.isBlank())
        {
            return null;
        }
        var lt = author.indexOf('<');
        var gt = author.lastIndexOf('>');
        if (lt > 0 && gt > lt)
        {
            var name = author.substring(0, lt).trim();
            var email = author.substring(lt + 1, gt).trim();
            return new PersonIdent(name, email);
        }
        return new PersonIdent(author.trim(), "");
    }
}
