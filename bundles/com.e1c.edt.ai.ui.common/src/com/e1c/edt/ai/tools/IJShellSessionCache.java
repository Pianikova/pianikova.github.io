/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

/**
 * Interface for JShell REPL session cache.
 */
public interface IJShellSessionCache
{
	/**
	 * Gets an existing session by ID or creates a new one.
	 * 
	 * @param sessionId the session ID, or null/empty to create a new session
	 * @return the JShell session
	 */
	IJShellSession getOrCreateSession(String sessionId);

	/**
	 * Invalidates a specific session.
	 * 
	 * @param sessionId the session ID to invalidate
	 */
	void invalidateSession(String sessionId);

	/**
	 * Invalidates all sessions.
	 */
	void invalidateAll();
}
