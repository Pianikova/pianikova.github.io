/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import java.util.Map;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IDiagnosticMapper
{
    DiagnosticResult map(String stage, int response, Throwable tRaw, Map<String, String> extraInfo);
}
