/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.Collection;
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
     * Returns description and usage examples for each binding.
     * This will be used to enrich the tool description.
     *
     * @return Map of variable names to binding descriptions
     */
    Map<String, JShellBindingDescription> getBindingInfos();

    /**
     * Returns a collection of significant classes that should be added to JShell classpath.
     * These classes are important for the bindings provided by this provider.
     *
     * @return Collection of significant classes
     */
    Collection<Class<?>> getSignificantClasses();

    /**
     * Returns array of import statements that should be pre-imported in JShell sessions.
     * This allows providers to contribute commonly used imports for their specific context.
     *
     * @return Array of import statements, or empty array if none
     */
    Collection<String> getImports();
}
