/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.StoredConfig;
import org.eclipse.jgit.util.FS;
import org.eclipse.jgit.util.SystemReader;

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
            .addParameter("<name> <value>", "Set a configuration variable")
            .addParameter("--unset <name>", "Remove a configuration variable")
            .addParameter("--list, -l", "List all configuration values in the selected scope")
            .addParameter("--local", "Use repository .git/config (default)")
            .addParameter("--global", "Use ~/.gitconfig")
            .addParameter("--system", "Use system /etc/gitconfig");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws Exception
    {
        var scope = Scope.LOCAL;
        var list = false;
        var unset = false;
        var get = false;
        var positional = new java.util.ArrayList<String>();

        for (var arg : args)
        {
            if (arg.equals("--global"))
            {
                scope = Scope.GLOBAL;
            }
            else if (arg.equals("--system"))
            {
                scope = Scope.SYSTEM;
            }
            else if (arg.equals("--local"))
            {
                scope = Scope.LOCAL;
            }
            else if (arg.equals("--list") || arg.equals("-l"))
            {
                list = true;
            }
            else if (arg.equals("--unset"))
            {
                unset = true;
            }
            else if (arg.equals("--get"))
            {
                get = true;
            }
            else
            {
                positional.add(arg);
            }
        }

        StoredConfig config = openConfig(git, scope);

        if (list)
        {
            var sb = new StringBuilder();
            for (var section : config.getSections())
            {
                for (var sub : config.getSubsections(section))
                {
                    for (var name : config.getNames(section, sub))
                    {
                        for (var value : config.getStringList(section, sub, name))
                        {
                            sb.append(section).append('.').append(sub).append('.').append(name)
                                .append('=').append(value).append('\n');
                        }
                    }
                }
                for (var name : config.getNames(section))
                {
                    for (var value : config.getStringList(section, null, name))
                    {
                        sb.append(section).append('.').append(name).append('=').append(value).append('\n');
                    }
                }
            }
            return new GitCommandResult(0, sb.toString(), "");
        }

        if (positional.isEmpty())
        {
            return new GitCommandResult(1, "", "fatal: no config key specified");
        }

        var key = positional.get(0);
        var parsed = parseKey(key);
        if (parsed == null)
        {
            return new GitCommandResult(1, "", "fatal: invalid key: " + key);
        }

        if (unset)
        {
            config.unset(parsed.section, parsed.subsection, parsed.name);
            config.save();
            return new GitCommandResult(0, "", "");
        }

        if (positional.size() == 1 || get)
        {
            var value = config.getString(parsed.section, parsed.subsection, parsed.name);
            if (value != null)
            {
                return new GitCommandResult(0, value + "\n", "");
            }
            return new GitCommandResult(1, "", "");
        }

        // Set value
        var value = positional.get(1);
        config.setString(parsed.section, parsed.subsection, parsed.name, value);
        config.save();
        return new GitCommandResult(0, "", "");
    }

    private static StoredConfig openConfig(Git git, Scope scope) throws Exception
    {
        switch (scope)
        {
            case GLOBAL:
                var user = SystemReader.getInstance().openUserConfig(null, FS.DETECTED);
                user.load();
                return user;
            case SYSTEM:
                var sys = SystemReader.getInstance().openSystemConfig(null, FS.DETECTED);
                sys.load();
                return sys;
            case LOCAL:
            default:
                return git.getRepository().getConfig();
        }
    }

    private static ParsedKey parseKey(String key)
    {
        var first = key.indexOf('.');
        if (first < 0)
        {
            return null;
        }
        var last = key.lastIndexOf('.');
        var section = key.substring(0, first);
        if (first == last)
        {
            return new ParsedKey(section, null, key.substring(first + 1));
        }
        var subsection = key.substring(first + 1, last);
        var name = key.substring(last + 1);
        return new ParsedKey(section, subsection, name);
    }

    private enum Scope
    {
        LOCAL, GLOBAL, SYSTEM
    }

    private static class ParsedKey
    {
        final String section;
        final String subsection;
        final String name;

        ParsedKey(String section, String subsection, String name)
        {
            this.section = section;
            this.subsection = subsection;
            this.name = name;
        }
    }

}
