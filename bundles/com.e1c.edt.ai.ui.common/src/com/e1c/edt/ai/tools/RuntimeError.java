/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import com.google.gson.annotations.SerializedName;

/**
 * Structured information about a JShell runtime exception.
 */
class RuntimeError
{
	@SerializedName("exception_type")
	public String exceptionType;

	@SerializedName("message")
	public String message;

	@SerializedName("stack_trace")
	public String stackTrace;
}
