/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.Set;

/**
 * Provides information about restricted/f forbidden types that should not be used in JShell.
 */
public interface IRestrictedTypesProvider
{
	/**
	 * Returns the set of fully qualified class names that are restricted from use.
	 *
	 * @return Set of restricted type names
	 */
	Set<String> getRestrictedTypes();

	/**
	 * Checks if a given type is restricted.
	 *
	 * @param typeName The fully qualified class name to check
	 * @return true if the type is restricted, false otherwise
	 */
	boolean isRestricted(String typeName);
}
