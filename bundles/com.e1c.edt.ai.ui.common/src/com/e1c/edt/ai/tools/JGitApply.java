/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;

/**
 * Git apply command implementation
 */
public class JGitApply implements IJGitCommand
{
    @SuppressWarnings("nls")
    @Override
    public String getName()
    {
        return "apply";
    }

    @SuppressWarnings("nls")
    @Override
    public JGitCommandDescription getDescription()
    {
        return new JGitCommandDescription("Apply a patch to files and/or to the index")
            .addParameter("<patch-content>", "Patch content to apply as string")
            .addParameter("(no arguments)", "Error: patch content required");
    }

    @SuppressWarnings("nls")
    @Override
    public GitCommandResult run(Git git, List<String> args) throws GitAPIException, IOException
    {
        var applyCmd = git.apply();
        var patchContent = "";

        // Parse arguments (JGit ApplyCommand has limited options compared to git)
        var patchBuilder = new StringBuilder();
        boolean isFirstNonFlag = true;
        for (var arg : args)
        {
            if (!arg.startsWith("-"))
            {
                // If first non-flag arg looks like a complete patch (contains diff header), use it as-is
                if (isFirstNonFlag && arg.contains("diff --git"))
                {
                    patchContent = arg;
                    break;
                }
                if (patchBuilder.length() > 0)
                {
                    patchBuilder.append("\n");
                }
                patchBuilder.append(arg);
                isFirstNonFlag = false;
            }
        }
        if (patchContent.isEmpty())
        {
            patchContent = patchBuilder.toString();
        }
        // Decode escaped newlines (\n -> actual newline)
        patchContent = patchContent.replace("\\n", "\n");

        // If patch content is provided as argument, use it
        if (!patchContent.isEmpty())
        {
            try (var input = new ByteArrayInputStream(patchContent.getBytes(StandardCharsets.UTF_8)))
            {
                applyCmd.setPatch(input);
                var result = applyCmd.call();
                if (result != null && !result.getUpdatedFiles().isEmpty())
                {
                    return new GitCommandResult(0, "", "");
                }
                else
                {
                    return new GitCommandResult(1, "", "fatal: patch could not be applied\n");
                }
            }
            catch (Exception e)
            {
                return new GitCommandResult(1, "", "fatal: failed to apply patch: " + e.getMessage() + "\n");
            }
        }
        else
        {
            return new GitCommandResult(1, "", "fatal: no patch content provided. Provide patch content as argument.\n"
                + "Note: JGit's apply command has limited options compared to git apply.");
        }
    }
}
