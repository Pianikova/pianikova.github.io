/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

/**
 * Result of Git command execution
 */
public class GitCommandResult
{
    public final int exitCode;
    public final String stdOut;
    public final String stdErr;

    public GitCommandResult(int exitCode, String stdOut, String stdErr)
    {
        this.exitCode = exitCode;
        this.stdOut = stdOut;
        this.stdErr = stdErr;
    }
}
