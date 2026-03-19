/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import com.google.gson.annotations.SerializedName;

/**
 * Result of JShell code execution.
 */
class JShellExecutionResult
{
	@SerializedName("repl_session_id")
	public String sessionId;

	@SerializedName("return_value")
	public String returnValue;

	@SerializedName("std_out")
	public String stdOut;

	@SerializedName("std_err")
	public String stdErr;

	@SerializedName("compilation_errors")
    public java.util.List<CompilationError> compilationErrors;

    @SerializedName("runtime_errors")
    public java.util.List<RuntimeError> runtimeErrors;

    @SerializedName("execution_history")
    public java.util.List<String> executionHistory;
}
