/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

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
	String getSessionId();

	/**
	 * Executes code in this JShell session.
	 * 
	 * @param code the Java code to execute
	 * @return the execution result
	 */
	JShellExecutionResult execute(String code);

	/**
	 * Closes the JShell session and releases resources.
	 */
	void close();
}
