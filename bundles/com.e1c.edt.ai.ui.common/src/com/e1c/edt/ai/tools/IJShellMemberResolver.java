/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.List;

public interface IJShellMemberResolver
{
    List<JShellReflectionSearchResult> findMembers(IJShellSession session, String query, int resultLimit, int itemLimit);
}
