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
	String getSessionId();

	/**
	 * Executes code in this JShell session.
	 *
	 * @param code the Java code to execute
	 * @return the execution result
	 */
	JShellExecutionResult execute(String code);

	/**
     * Returns session result with available bindings.
     *
     * @return session result
     */
    SessionResult getSessionResult();

    /**
     * Returns class loader used by this session for resolving Java API types.
     *
     * @return session class loader
     */
    ClassLoader getClassLoader();

    /**
     * Returns imports available in this session.
     *
     * @return list of import statements
     */
    List<String> getImports();

    /**
     * Closes the JShell session and releases resources.
     */
	void close();
}
