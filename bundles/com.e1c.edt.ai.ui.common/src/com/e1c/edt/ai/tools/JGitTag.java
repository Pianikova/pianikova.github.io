/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.Comparator;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;

/**
 * Git tag command implementation
 */
public class JGitTag implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "tag"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Create, list, or delete a tag object")
            .addParameter("<tagname>", "Create a lightweight tag with this name")
            .addParameter("-a, --annotate", "Make an annotated tag")
            .addParameter("-m <msg>", "Message for the annotated tag")
            .addParameter("-d, --delete <tagname>...", "Delete one or more existing tags")
            .addParameter("-l, --list [<pattern>]", "List tags (optionally matching pattern)")
            .addParameter("--sort=<key>",
                "Sort results: version:refname (version-aware), -version:refname (descending), creatordate")
            .addParameter("(no arguments)", "List all tags");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException
    {
        var delete = false;
        var list = false;
        var annotated = false;
        String message = "";
        String sortKey = null;
        String pattern = null;
        String tagName = null;
        var toDelete = new java.util.ArrayList<String>();

        for (int i = 0; i < args.size(); i++)
        {
            var arg = args.get(i);
            if (arg.equals("-d") || arg.equals("--delete"))
            {
                delete = true;
            }
            else if (arg.equals("-l") || arg.equals("--list"))
            {
                list = true;
                if (i + 1 < args.size() && !args.get(i + 1).startsWith("-"))
                {
                    pattern = args.get(++i);
                }
            }
            else if (arg.equals("-a") || arg.equals("--annotate"))
            {
                annotated = true;
            }
            else if (arg.equals("-m") && i + 1 < args.size())
            {
                message = args.get(++i);
            }
            else if (arg.startsWith("--sort="))
            {
                sortKey = arg.substring("--sort=".length());
            }
            else if (!arg.startsWith("-"))
            {
                if (delete)
                {
                    toDelete.add(arg);
                }
                else if (tagName == null)
                {
                    tagName = arg;
                }
            }
        }

        // Delete
        if (delete)
        {
            if (toDelete.isEmpty())
            {
                return new GitCommandResult(1, "", "error: tag name required\n");
            }
            git.tagDelete().setTags(toDelete.toArray(new String[0])).call();
            var sb = new StringBuilder();
            for (var t : toDelete)
            {
                sb.append("Deleted tag '").append(t).append("'\n");
            }
            return new GitCommandResult(0, sb.toString(), "");
        }

        // List (or no arguments)
        if (list || tagName == null)
        {
            var tags = git.tagList().call();

            // Resolve display names
            var names = new java.util.ArrayList<String>();
            for (var ref : tags)
            {
                var name = ref.getName();
                if (name.startsWith(Constants.R_TAGS))
                {
                    name = name.substring(Constants.R_TAGS.length());
                }
                if (pattern != null && !matchesGlob(name, pattern))
                {
                    continue;
                }
                names.add(name);
            }

            if (sortKey != null)
            {
                Comparator<String> cmp;
                switch (sortKey)
                {
                    case "version:refname":
                        cmp = JGitTag::compareVersions;
                        break;
                    case "-version:refname":
                        cmp = (a, b) -> compareVersions(b, a);
                        break;
                    default:
                        cmp = Comparator.naturalOrder();
                        break;
                }
                names.sort(cmp);
            }

            var sb = new StringBuilder();
            for (var n : names)
            {
                sb.append(n).append("\n");
            }
            return new GitCommandResult(0, sb.toString(), "");
        }

        // Create
        var tagCmd = git.tag();
        tagCmd.setName(tagName);
        if (annotated)
        {
            tagCmd.setMessage(message.isEmpty() ? tagName : message);
        }
        tagCmd.call();
        return new GitCommandResult(0, "", "");
    }

    /** Simple glob matching: only '*' is supported as a wildcard. */
    @SuppressWarnings("nls")
    private static boolean matchesGlob(String name, String pattern)
    {
        var regex = "\\Q" + pattern.replace("*", "\\E.*\\Q") + "\\E";
        return name.matches(regex);
    }

    /** Lexicographic version-aware comparison (splits on '.', '-', '~'). */
    @SuppressWarnings("nls")
    private static int compareVersions(String a, String b)
    {
        var pa = a.split("[.\\-~]");
        var pb = b.split("[.\\-~]");
        int len = Math.min(pa.length, pb.length);
        for (int i = 0; i < len; i++)
        {
            int cmp;
            try
            {
                cmp = Integer.compare(Integer.parseInt(pa[i]), Integer.parseInt(pb[i]));
            }
            catch (NumberFormatException e)
            {
                cmp = pa[i].compareTo(pb[i]);
            }
            if (cmp != 0)
            {
                return cmp;
            }
        }
        return Integer.compare(pa.length, pb.length);
    }
}
