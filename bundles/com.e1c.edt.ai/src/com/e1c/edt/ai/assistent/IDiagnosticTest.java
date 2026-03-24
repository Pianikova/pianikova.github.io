/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import org.eclipse.core.runtime.IProgressMonitor;


/**
 * @author Bogdan Sushkov
 *
 */
public interface IDiagnosticTest
{
    String id();

    String title();

    DiagnosticResult execute(IDiagnosticContext context, IProgressMonitor monitor);
}
