/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import com.google.gson.annotations.SerializedName;

/**
 * Structured information about a JShell compilation error.
 */
class CompilationError
{
	@SerializedName("is_error")
	public boolean isError;

	@SerializedName("code")
	public String code;

	@SerializedName("message")
	public String message;

	@SerializedName("position")
	public long position;

	@SerializedName("start_position")
	public long startPosition;

	@SerializedName("end_position")
	public long endPosition;

	@SerializedName("is_resolution_error")
	public boolean isResolutionError;

	@SerializedName("is_unreachable_error")
	public boolean isUnreachableError;

	@SerializedName("is_not_a_statement_error")
	public boolean isNotAStatementError;
}
