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
     * Returns the execution scope this provider supports.
     * <p>
     * The scope is used by JShell tool calls to select provider-specific workflow hints.
     *
     * @return Scope name, for example {@code edt} or {@code eclipse}
     */
    String getScope();

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
     * Returns import statements that should be pre-imported in JShell sessions.
     * <p>
     * Keep this list curated and scope-specific: prefer explicit imports for stable,
     * frequently used API types, avoid wildcard imports, and avoid importing classes
     * with ambiguous simple names. The list is a convenience baseline, not a complete
     * replacement for scenario-specific manual guidance.
     *
     * @return Import statements, or empty collection if none
     */
    Collection<String> getImports();

    /**
     * Returns use cases description for this binding provider.
     * This describes the typical scenarios and workflows where this provider's bindings are useful.
     *
     * @return Use cases description string, or empty string if none
     */
    String getUseCases();

    /**
     * Returns provider-specific required next step after JShell code execution.
     *
     * @param context Execution context
     * @return Required next step text, or empty string if no next step is required
     */
    default String getRequiredNextStep(JShellExecutionContext context)
    {
        return ""; //$NON-NLS-1$
    }
}
