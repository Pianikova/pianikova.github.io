/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.List;

public interface IJShellTypeIndex
{
    List<JShellResolvedType> findTypes(IJShellSession session, String query, int limit);

    List<JShellResolvedType> findPackageTypes(IJShellSession session, String packageName, int limit);

    boolean hasPackage(String packageName);
}
