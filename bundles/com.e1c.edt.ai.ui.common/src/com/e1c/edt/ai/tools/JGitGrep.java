/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.treewalk.TreeWalk;

/**
 * Git grep command implementation
 */
public class JGitGrep implements IJGitCommand
{
    @Override
    public String getName()
    {
        return "grep"; //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription(
            "Print lines matching a pattern in tracked files (working tree or a revision)")
                .addParameter("<pattern>", "Regex pattern (Java syntax)")
                .addParameter("<rev>", "Optional revision to search; default is the working tree")
                .addParameter("-- <pathspec>...", "Limit to files containing any of the given path substrings")
                .addParameter("-i, --ignore-case", "Case insensitive matching")
                .addParameter("-l, --files-with-matches", "Show only names of files with matches")
                .addParameter("-c, --count", "Show count of matches per file instead of matching lines")
                .addParameter("-w, --word-regexp", "Match the pattern only as a whole word")
                .addParameter("-n, --line-number", "Prefix matches with line numbers (default: on)");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws Exception
    {
        var ignoreCase = false;
        var filesOnly = false;
        var countOnly = false;
        var wholeWord = false;
        var showLineNumbers = true;
        String pattern = null;
        String rev = null;
        var paths = new ArrayList<String>();
        var afterDoubleDash = false;

        for (int i = 0; i < args.size(); i++)
        {
            var arg = args.get(i);
            if (afterDoubleDash)
            {
                paths.add(arg);
                continue;
            }
            if (arg.equals("--"))
            {
                afterDoubleDash = true;
            }
            else if (arg.equals("-i") || arg.equals("--ignore-case"))
            {
                ignoreCase = true;
            }
            else if (arg.equals("-l") || arg.equals("--files-with-matches"))
            {
                filesOnly = true;
            }
            else if (arg.equals("-c") || arg.equals("--count"))
            {
                countOnly = true;
            }
            else if (arg.equals("-w") || arg.equals("--word-regexp"))
            {
                wholeWord = true;
            }
            else if (arg.equals("-n") || arg.equals("--line-number"))
            {
                showLineNumbers = true;
            }
            else if (arg.equals("-h") || arg.equals("--no-line-number"))
            {
                showLineNumbers = false;
            }
            else if (!arg.startsWith("-"))
            {
                if (pattern == null)
                {
                    pattern = arg;
                }
                else if (rev == null)
                {
                    rev = arg;
                }
                else
                {
                    paths.add(arg);
                }
            }
        }

        if (pattern == null)
        {
            return new GitCommandResult(1, "", "fatal: missing <pattern>");
        }

        var regex = wholeWord ? "\\b" + pattern + "\\b" : pattern;
        var flags = ignoreCase ? Pattern.CASE_INSENSITIVE : 0;
        var compiled = Pattern.compile(regex, flags);

        var repository = git.getRepository();
        var sb = new StringBuilder();

        if (rev == null)
        {
            grepWorkingTree(repository, compiled, paths, filesOnly, countOnly, showLineNumbers, sb);
        }
        else
        {
            var revId = repository.resolve(rev);
            if (revId == null)
            {
                return new GitCommandResult(128, "", "fatal: bad revision '" + rev + "'");
            }
            grepRevision(repository, revId, compiled, paths, filesOnly, countOnly, showLineNumbers, sb);
        }

        if (sb.length() == 0)
        {
            return new GitCommandResult(1, "", "");
        }
        return new GitCommandResult(0, sb.toString(), "");
    }

    private static void grepWorkingTree(Repository repository, Pattern pattern, List<String> paths,
        boolean filesOnly, boolean countOnly, boolean lineNumbers, StringBuilder out) throws IOException
    {
        var dirCache = repository.readDirCache();
        var workTree = repository.getWorkTree();
        for (int i = 0; i < dirCache.getEntryCount(); i++)
        {
            var entry = dirCache.getEntry(i);
            var path = entry.getPathString();
            if (!matchesPaths(path, paths))
            {
                continue;
            }
            var file = new java.io.File(workTree, path);
            if (!file.isFile())
            {
                continue;
            }
            byte[] bytes;
            try
            {
                bytes = Files.readAllBytes(file.toPath());
            }
            catch (IOException e)
            {
                continue;
            }
            grepBytes(path, bytes, pattern, filesOnly, countOnly, lineNumbers, null, out);
        }
    }

    private static void grepRevision(Repository repository, ObjectId revId, Pattern pattern, List<String> paths,
        boolean filesOnly, boolean countOnly, boolean lineNumbers, StringBuilder out) throws IOException
    {
        try (var revWalk = new RevWalk(repository);
            var treeWalk = new TreeWalk(repository);
            var reader = repository.newObjectReader())
        {
            var commit = revWalk.parseCommit(revId);
            treeWalk.addTree(commit.getTree());
            treeWalk.setRecursive(true);
            while (treeWalk.next())
            {
                var path = treeWalk.getPathString();
                if (!matchesPaths(path, paths))
                {
                    continue;
                }
                var loader = reader.open(treeWalk.getObjectId(0));
                var bytes = loader.getBytes();
                grepBytes(path, bytes, pattern, filesOnly, countOnly, lineNumbers, revId.getName(), out);
            }
        }
    }

    @SuppressWarnings("nls")
    private static void grepBytes(String path, byte[] bytes, Pattern pattern, boolean filesOnly,
        boolean countOnly, boolean lineNumbers, String revName, StringBuilder out)
    {
        if (looksBinary(bytes))
        {
            return;
        }
        var content = new String(bytes, StandardCharsets.UTF_8);
        var lines = content.split("\n", -1);
        var prefix = revName == null ? path : revName + ":" + path;
        var count = 0;
        for (int ln = 0; ln < lines.length; ln++)
        {
            var line = lines[ln];
            if (pattern.matcher(line).find())
            {
                count++;
                if (filesOnly)
                {
                    out.append(prefix).append('\n');
                    return;
                }
                if (!countOnly)
                {
                    out.append(prefix).append(':');
                    if (lineNumbers)
                    {
                        out.append(ln + 1).append(':');
                    }
                    out.append(line).append('\n');
                }
            }
        }
        if (countOnly && count > 0)
        {
            out.append(prefix).append(':').append(count).append('\n');
        }
    }

    private static boolean looksBinary(byte[] bytes)
    {
        var limit = Math.min(bytes.length, 8000);
        for (int i = 0; i < limit; i++)
        {
            if (bytes[i] == 0)
            {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesPaths(String path, List<String> paths)
    {
        if (paths.isEmpty())
        {
            return true;
        }
        for (var p : paths)
        {
            if (path.contains(p))
            {
                return true;
            }
        }
        return false;
    }
}
