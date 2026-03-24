/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import java.util.List;


/**
 * @author Bogdan Sushkov
 *
 */
public class DiagnosticsFactory
    implements IDiagnosticsFactory
{
    @Override
    //formatter:off
    public List<IDiagnosticTest> createDiagnostics()
    {
        return List.of(
            new HealthCheckDiagnosticTest(),
            new TokenDiagnosticTest(),
            new SessionDiagnosticTest(),
            new CodeCompletionDiagnosticTest(),
            new ChatDiagnosticTest()
         );
    }
    //formatter:on
}
