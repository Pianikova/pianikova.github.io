/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.List;

/**
 * Interface for JShell REPL session.
 */
public interface IJShellSession
{
	/**
	 * Returns the unique session ID.
	 *
	 * @return the session ID
	 */
	int getSessionId();

	/**
	 * Executes code in this JShell session.
	 *
	 * @param code the Java code to execute
	 * @return the execution result
	 */
	JShellExecutionResult execute(String code);

	/**
	 * Returns the history of code executed in this session.
	 *
	 * @return list of executed code snippets
	 */
	List<String> getExecutionHistory();

	/**
	 * Closes the JShell session and releases resources.
	 */
	void close();
}
