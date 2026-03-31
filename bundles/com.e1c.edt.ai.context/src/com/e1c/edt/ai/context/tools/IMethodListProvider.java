/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools;

import java.util.List;

/**
 * Provider for retrieving method signatures from classes.
 */
public interface IMethodListProvider
{
	/**
	 * Retrieves public method signatures from the specified class.
	 *
	 * @param clazz the class to retrieve methods from
	 * @return list of public method signatures
	 */
	List<String> getPublicMethodSignatures(Class<?> clazz);
}
