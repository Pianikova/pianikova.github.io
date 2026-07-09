/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.Collection;
import java.util.List;

import com.e1c.edt.ai.ICancellationToken;

public interface IJShellTypeIndex
{
    void warmUp(Collection<Class<?>> significantClasses, ICancellationToken cancellationToken);

    List<JShellResolvedType> findTypes(IJShellSession session, String query, int limit);

    List<JShellResolvedType> findPackageTypes(IJShellSession session, String packageName, int limit);

    boolean hasPackage(String packageName);
}
