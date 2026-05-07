/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

public class SessionResult
{
	@SerializedName("repl_session_id")
	public String sessionId;

	@SerializedName("available_bindings")
	public ArrayList<String> availableBindings;

	@SerializedName("execution_history")
	public ArrayList<String> executionHistory;
}
