/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import com.e1c.edt.ai.ToolException;

/**
 * Validates Java code for usage of restricted types.
 *
 * This validator parses Java code to identify potential usage of restricted types
 * before execution in JShell.
 */
public interface IRestrictedTypesValidator
{
	/**
	 * Validates code for restricted type usage.
	 *
	 * @param code The Java code to validate
	 * @throws ToolException if a restricted type is found
	 */
	void validate(String code) throws ToolException;
}
