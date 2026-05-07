/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import com.google.gson.annotations.SerializedName;

public class SessionResult
{
	@SerializedName("repl_session_id")
	public String sessionId;

	@SerializedName("available_bindings")
	public java.util.List<String> availableBindings;
}
