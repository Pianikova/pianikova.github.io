/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.tools.metadata;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

final class MetadataOperationDescriptor
{
    final String name;
    final String description;
    final Set<String> requiredParameters;
    final Set<String> optionalParameters;
    final String example;

    MetadataOperationDescriptor(String name, String description, Set<String> requiredParameters,
        Set<String> optionalParameters, String example)
    {
        this.name = name;
        this.description = description;
        this.requiredParameters = immutable(requiredParameters);
        this.optionalParameters = immutable(optionalParameters);
        this.example = example;
    }

    /**
     * Parameters accepted by every operation, in addition to its own: {@code operation} itself and
     * {@code verify} (post-mutation marker auto-check, enabled unless explicitly disabled).
     */
    static final Set<String> COMMON_PARAMETERS = Set.of("operation", "verify"); //$NON-NLS-1$ //$NON-NLS-2$

    Set<String> allParameters()
    {
        var result = new LinkedHashSet<String>();
        result.addAll(COMMON_PARAMETERS);
        result.addAll(requiredParameters);
        result.addAll(optionalParameters);
        return result;
    }

    private static Set<String> immutable(Set<String> values)
    {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }
}
