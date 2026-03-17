/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.Map;

/**
 * Interface for providing JShell bindings from runtime.
 * Implementations can register objects that will be available in JShell REPL.
 */
public interface IJShellBindingProvider
{
	/**
	 * Returns a map of variable names to objects that should be bound to JShell.
	 * The map keys are variable names (as they will appear in JShell),
	 * and values are the actual objects.
	 * 
	 * @return Map of variable names to objects
	 */
	Map<String, Object> getBindings();

	/**
	 * Returns a description of each binding for documentation purposes.
	 * This will be used to enrich the tool description.
	 * 
	 * @return Map of variable names to their descriptions
	 */
	default Map<String, String> getBindingDescriptions()
	{
		return new java.util.HashMap<>();
	}
}
