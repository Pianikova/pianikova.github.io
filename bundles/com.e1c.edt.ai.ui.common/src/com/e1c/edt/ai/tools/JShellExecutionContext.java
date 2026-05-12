/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

/**
 * Context passed to JShell binding providers after code execution.
 */
public class JShellExecutionContext
{
    public String scope;

    public String requestDescription;

    public String responseDescription;

    public String code;

    public JShellExecutionResult result;
}
