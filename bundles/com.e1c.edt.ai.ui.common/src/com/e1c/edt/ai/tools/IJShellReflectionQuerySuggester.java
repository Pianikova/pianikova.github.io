/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.List;

public interface IJShellReflectionQuerySuggester
{
    List<String> suggestForQuery(String query, int limit);

    List<String> suggestForCompilationErrors(String code, List<CompilationError> errors, int limit);
}
