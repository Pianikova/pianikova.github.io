/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

/**
 * Interface for matching file paths against glob patterns.
 */
public interface IPatternMatcher
{
	/**
	 * Checks if the given path matches the glob pattern.
	 *
	 * @param path the relative file path to check
	 * @param pattern the glob pattern to match against
	 * @return true if the path matches the pattern, false otherwise
	 */
	boolean matches(String path, String pattern);
}
