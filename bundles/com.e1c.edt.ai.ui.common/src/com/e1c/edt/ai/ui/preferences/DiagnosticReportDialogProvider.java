/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.preferences;

import java.time.ZonedDateTime;
import java.util.Map;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.swt.widgets.Shell;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.assistent.DiagnosticResult;
import com.e1c.edt.ai.assistent.DiagnosticSeverity;
import com.e1c.edt.ai.assistent.IDiagnosticContext;
import com.e1c.edt.ai.assistent.IDiagnosticTest;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class DiagnosticReportDialogProvider
    implements IDiagnosticReportDialogProvider
{
    private static final String PLUGIN_ID = "com.e1c.edt.ai"; //$NON-NLS-1$
    private static final String SEP = System.lineSeparator() + "------------------------" + System.lineSeparator(); //$NON-NLS-1$
    private final ISettings settings;

    @Inject
    public DiagnosticReportDialogProvider(ISettings settings)
    {
        Preconditions.checkNotNull(settings);
        this.settings = settings;
    }

    @Override
    public void openErrorDialog(Shell shell, IDiagnosticTest test, DiagnosticResult r, IDiagnosticContext ctx)
    {

        String title = Messages.DiagnosticReportDialog_Report;
        String msg = safe(r.getMessage());
        if (msg.isBlank())
        {
            msg = (test != null ? test.title() : Messages.DiagnosticReportDialog_Test + " ") //$NON-NLS-1$
                + Messages.DiagnosticReportDialog_EmptyMessage;
        }

        int severity = toIStatusSeverity(r.getSeverity());

        IStatus status = new Status(severity, PLUGIN_ID, msg);

        msg = (test != null ? test.title() : Messages.DiagnosticReportDialog_Test + " ") + " " //$NON-NLS-1$ //$NON-NLS-2$
            + Messages.TestReportDialog_DiagnosticNotPassed;

        int mask = IStatus.ERROR | IStatus.WARNING | IStatus.INFO | IStatus.OK;

        String tail =  r.getRemidiation().getUrlPath();
        if (tail == null || tail.isEmpty()) {
            tail = "troubleshooting/"; //$NON-NLS-1$
        }
        String url = settings.getHomePage() + tail;

        // support log exported to file (includes technical data)
        String supportLogText = buildSupportLog(test, r, ctx);

        new TestReportDialog(shell, title, msg, status, mask, url, supportLogText).open();
    }

    private int toIStatusSeverity(DiagnosticSeverity s)
    {
        if (s == null)
            return IStatus.ERROR;
        switch (s)
        {
        case OK:
            return IStatus.OK;
        case DEFAULT:
            return IStatus.CANCEL;
        case ERROR:
        default:
            return IStatus.ERROR;
        }
    }

    @SuppressWarnings("nls")
    private String buildSupportLog(IDiagnosticTest test, DiagnosticResult r, IDiagnosticContext ctx)
    {
        if (ctx != null && ctx.getCAReport() == null)
        {
            ctx.setCaReportIfAbsent(ctx.getCaCertificateReporter().buildPlainLog());
        }

        StringBuilder sb = new StringBuilder(48_000);

        sb.append("=== AI DIAGNOSTICS LOG ===\n");
        sb.append("time=").append(ZonedDateTime.now()).append("\n");
        sb.append("test.id=").append(test != null ? safe(test.id()) : "").append("\n");
        sb.append("test.title=").append(test != null ? safe(test.title()) : "").append("\n");
        sb.append("severity=").append(String.valueOf(r.getSeverity())).append("\n\n");

        sb.append("[USER MESSAGE]\n");
        sb.append(safe(r.getMessage())).append("\n\n");

        sb.append("[FACTS]\n");
        Map<String, String> facts = r.getFacts();
        if (facts != null && !facts.isEmpty())
        {
            for (Map.Entry<String, String> e : facts.entrySet())
            {
                sb.append(safe(e.getKey())).append(" = ").append(safe(e.getValue())).append("\n");
        }
        }
        else
        {
            sb.append("(no facts)\n");
        }
        sb.append("java.version = ").append(System.getProperty("java.version")).append("\n");
        sb.append("java.vendor  = ").append(System.getProperty("java.vendor")).append("\n");
        sb.append("java.home    = ").append(System.getProperty("java.home")).append("\n\n");

        sb.append("[TECHNICAL - FOR SUPPORT ONLY]\n");
        if (r.getTechnical() != null)
        {
            sb.append("exception=").append(r.getTechnical().getClass().getName()).append("\n");
            sb.append("message=").append(String.valueOf(r.getTechnical().getMessage())).append("\n");
        }
        sb.append(SEP);

        String log = safe(r.getLog());
        if (!log.isBlank())
        {
            sb.append(log).append("\n");
        }
        else
        {
            sb.append("(no stacktrace captured)\n");
        }
        sb.append("\n");

        sb.append("[CA CERTIFICATES - TRUSTSTORE]\n");
        if (ctx != null && ctx.getCAReport() != null)
        {
            sb.append(ctx.getCAReport());
        }
        else
        {
            sb.append("(CA report not collected)\n");
        }

        sb.append("\n=== END AI DIAGNOSTICS LOG ===\n");
        return sb.toString();
    }

    private String safe(String s)
    {
        return s == null ? "" : s; //$NON-NLS-1$
    }
}
