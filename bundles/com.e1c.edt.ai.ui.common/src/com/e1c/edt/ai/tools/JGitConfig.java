/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;

/**
 * Git config command implementation
 */
public class JGitConfig implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "config"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Get and set repository or global options")
            .addParameter("--get <name>", "Get the value of a configuration variable")
            .addParameter("<name>", "Same as --get")
            .addParameter("<name> <value>", "Set a configuration variable");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws IOException
    {
        if (args.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: no config key specified");
        }

        var hasGetFlag = args.contains("--get");
        var get = hasGetFlag || !args.contains("--") && args.size() == 1;
        var keyIndex = hasGetFlag ? args.indexOf("--get") + 1 : 0;

        if (keyIndex >= args.size())
        {
            return new GitCommandResult(1, "", "fatal: no config key specified");
        }

        var key = args.get(keyIndex);

        if (get)
        {
            var config = git.getRepository().getConfig();
            var value = config.getString(key.split("\\.")[0], null, key.substring(key.indexOf('.') + 1));

            if (value != null)
            {
                return new GitCommandResult(0, value + "\n", "");
            }
            else
            {
                return new GitCommandResult(1, "", "");
            }
        }

        return new GitCommandResult(1, "", "fatal: config operation not supported");
    }
}
