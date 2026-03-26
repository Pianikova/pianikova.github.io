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
     * Returns description and usage examples for each binding.
     * This will be used to enrich the tool description.
     *
     * @return Map of variable names to binding descriptions
     */
    Map<String, JShellBindingDescription> getBindings();

    /**
     * Returns a brief description of this binding provider.
     * This describes what kind of bindings this provider offers.
     *
     * @return Brief description of the provider
     */
    String getDescription();

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

    /**
     * Returns use cases description for this binding provider.
     * This describes the typical scenarios and workflows where this provider's bindings are useful.
     *
     * @return Use cases description string, or empty string if none
     */
    String getUseCases();
}
