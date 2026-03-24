/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.preferences;

import org.eclipse.swt.widgets.Shell;

import com.e1c.edt.ai.assistent.DiagnosticResult;
import com.e1c.edt.ai.assistent.IDiagnosticContext;
import com.e1c.edt.ai.assistent.IDiagnosticTest;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IDiagnosticReportDialogProvider
{
    /**
     * Opens a dialog for displaying diagnostic results if diagnostic ended with errors.
     *
     * @param shell
     * @param test
     * @param r
     * @param ctx
     */
    void openErrorDialog(Shell shell, IDiagnosticTest test, DiagnosticResult r, IDiagnosticContext ctx);
}
