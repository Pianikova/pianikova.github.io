/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.List;

public interface IJShellReflectionService
{
    List<JShellReflectionQueryResult> search(IJShellSession session, List<String> queries);
}
