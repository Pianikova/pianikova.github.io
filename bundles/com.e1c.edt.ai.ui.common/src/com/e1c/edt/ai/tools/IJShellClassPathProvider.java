/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import jdk.jshell.JShell;

/**
 * Interface for providing classpath configuration for JShell sessions.
 */
public interface IJShellClassPathProvider
{
	/**
	 * Adds classpath entries to the JShell instance for a specific class.
	 *
	 * @param shell the JShell instance
	 * @param clazz the class whose classpath should be added
	 */
	void addClassPathFor(JShell shell, Class<?> clazz);

	/**
	 * Adds all bundle classpaths to the JShell instance.
	 *
	 * @param shell the JShell instance
	 */
	void addAllBundleClassPaths(JShell shell);
}
