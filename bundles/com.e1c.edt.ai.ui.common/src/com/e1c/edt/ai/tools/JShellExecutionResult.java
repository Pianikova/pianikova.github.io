/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * Result of JShell code execution.
 */
class JShellExecutionResult
{
	@SerializedName("repl_session_id")
    public int sessionId;

	@SerializedName("std_out")
	public String stdOut;

	@SerializedName("std_err")
	public String stdErr;

	@SerializedName("compilation_errors")
    public List<CompilationError> compilationErrors;

    @SerializedName("runtime_errors")
    public List<RuntimeError> runtimeErrors;

    @SerializedName("execution_history")
    public List<String> executionHistory;
}
