/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.util.List;

import org.eclipse.jgit.api.Git;

/**
 * Interface for Git command implementations
 */
public interface IJGitCommand
{
    /**
     * Get the command name
     * @return command name (e.g., "status", "add", "commit")
     */
    String getName();

    /**
     * Get the command description for documentation
     * @return command description
     */
    JGitCommandDescription getDescription();

    /**
     * Execute the Git command
     * @param git Git instance
     * @param args command arguments (excluding the command name itself)
     * @return execution result
     * @throws Exception if execution fails
     */
    GitCommandResult run(Git git, List<String> args) throws Exception;
}
