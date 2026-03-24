/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

/**
 * Interface for JShell REPL session cache.
 */
public interface IJShellSessionManager
{
	/**
	 * Gets an existing session by ID or creates a new one.
	 *
	 * @param sessionId the session ID, or 0 to create a new session
	 * @return the JShell session
	 */
	IJShellSession getOrCreateSession(int sessionId);

	/**
	 * Gets an existing session by ID without creating a new one.
	 *
	 * @param sessionId the session ID
	 * @return the JShell session, or null if not found
	 */
	IJShellSession getSession(int sessionId);

	/**
	 * Invalidates a specific session.
	 *
	 * @param sessionId the session ID to invalidate
	 */
	void invalidateSession(int sessionId);

	/**
	 * Invalidates all sessions.
	 */
	void invalidateAll();
}
