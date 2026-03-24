///*
// * Copyright (c) 2026, ООО 1С-Софт
// */
//package com.e1c.edt.ai.ui.preferences;
//
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Files;
//import java.nio.file.Path;
//
//import org.eclipse.core.runtime.IStatus;
//import org.eclipse.jface.dialogs.ErrorDialog;
//import org.eclipse.jface.dialogs.IDialogConstants;
//import org.eclipse.jface.dialogs.MessageDialog;
//import org.eclipse.swt.SWT;
//import org.eclipse.swt.graphics.Point;
//import org.eclipse.swt.layout.GridData;
//import org.eclipse.swt.layout.GridLayout;
//import org.eclipse.swt.widgets.Button;
//import org.eclipse.swt.widgets.Composite;
//import org.eclipse.swt.widgets.Control;
//import org.eclipse.swt.widgets.FileDialog;
//import org.eclipse.swt.widgets.Shell;
//import org.eclipse.swt.widgets.Text;
//
///**
// * @author Bogdan Sushkov
// *
// */
//public class TestReportDialog
//    extends ErrorDialog
//{
//    private static final int STEPS_ID = IDialogConstants.CANCEL_ID + 10;
//    private static final int EXPORT_LOG_ID = IDialogConstants.CANCEL_ID + 11;
//
//    private enum PanelMode
//    {
//        NONE,
//        STEPS
//    }
//
//    private PanelMode panelMode = PanelMode.NONE;
//    private final String url;
//    private final String supportLogText;
//    private Button stepsButton;
//
//    private Composite panel;
//    private Text panelText;
//
//    /**
//     * @param parentShell
//     * @param dialogTitle
//     * @param message
//     * @param status
//     * @param displayMask
//     */
//    public TestReportDialog(Shell parentShell, String dialogTitle, String message, IStatus status, int displayMask,
//        String url, String supportLogText)
//    {
//        super(parentShell, dialogTitle, message, status, displayMask);
//        this.url = url;
//        this.supportLogText = supportLogText;
//    }
//
//    @Override
//    protected Control createButtonBar(Composite parent)
//    {
//        Composite bar = new Composite(parent, SWT.NONE);
//        bar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
//        GridLayout layout = new GridLayout(1, false);
//        layout.marginHeight = 0;
//        layout.marginWidth = 0;
//        layout.verticalSpacing = 6;
//        bar.setLayout(layout);
//
//        // buttons row
//        Composite buttonsRow = new Composite(bar, SWT.NONE);
//        buttonsRow.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
//        GridLayout buttonsRowLayout = new GridLayout(3, false);
//        buttonsRow.setLayout(buttonsRowLayout);
//
//        stepsButton = createButton(buttonsRow, STEPS_ID, Messages.TestReportDialog_HowToFix_Open, false);
//        createButton(buttonsRow, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
//        createButton(buttonsRow, EXPORT_LOG_ID, Messages.TestReportDialog_ExportLog_Button, false);
//
//        panel = new Composite(bar, SWT.NONE);
//        GridData panelGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
//        panelGridData.exclude = true;
//        panel.setLayoutData(panelGridData);
//        panel.setVisible(false);
//
//        GridLayout panelLayout = new GridLayout(1, false);
//        panelLayout.marginHeight = 0;
//        panelLayout.marginWidth = 0;
//        panel.setLayout(panelLayout);
//
//        panelText = new Text(panel, SWT.BORDER | SWT.READ_ONLY | SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
//        GridData panelTextGridData = new GridData(SWT.FILL, SWT.FILL, true, true);
//        panelTextGridData.heightHint = 160;
//        panelText.setLayoutData(panelTextGridData);
//
//        return bar;
//
//    }
//
//    @Override
//    protected void buttonPressed(int id)
//    {
//        if (id == STEPS_ID)
//        {
//            toggleSteps();
//            return;
//        }
//        if (id == EXPORT_LOG_ID)
//        {
//            exportSupportLog();
//            return;
//        }
//        super.buttonPressed(id);
//    }
//
//    private void toggleSteps()
//    {
//        if (panel == null || panel.isDisposed())
//            return;
//
//        panelMode = (panelMode == PanelMode.STEPS) ? PanelMode.NONE : PanelMode.STEPS;
//
//        if (panelMode == PanelMode.NONE)
//        {
//            setPanelVisible(false, ""); //$NON-NLS-1$
//        }
//        else
//        {
//            String t =
//                (stepsText == null || stepsText.isBlank()) ? Messages.TestReportDialog_StepsNotPresented : stepsText;
//            setPanelVisible(true, t);
//        }
//
//        if (stepsButton != null && !stepsButton.isDisposed())
//        {
//            stepsButton.setText(panelMode == PanelMode.STEPS ? Messages.TestReportDialog_HowToFix_Close
//                : Messages.TestReportDialog_HowToFix_Open);
//        }
//
//        relayoutAndResize();
//    }
//
//    private void exportSupportLog()
//    {
//        Shell shell = getShell();
//        if (shell == null || shell.isDisposed())
//            return;
//
//        FileDialog fd = new FileDialog(shell, SWT.SAVE);
//        fd.setText(Messages.TestReportDialog_ExportLog_Title);
//        fd.setFilterExtensions(new String[] { "*.log" }); //$NON-NLS-1$
//        fd.setFilterNames(new String[] { "Log file (*.log)" }); //$NON-NLS-1$
//        fd.setOverwrite(true);
//        fd.setFileName("diagnostics.log"); //$NON-NLS-1$
//
//        String pathStr = fd.open();
//        if (pathStr == null || pathStr.isBlank())
//            return;
//
//        try
//        {
//            Files.writeString(Path.of(pathStr), supportLogText == null ? "" : supportLogText, StandardCharsets.UTF_8); //$NON-NLS-1$
//        }
//        catch (Exception e)
//        {
//            MessageDialog.openError(shell, Messages.TestReportDialog_Error,
//                Messages.TestReportDialog_ExportLog_Error + e.getMessage());
//        }
//    }
//
//    private void setPanelVisible(boolean visible, String text)
//    {
//        panelText.setText(text == null ? "" : text); //$NON-NLS-1$
//
//        GridData gd = (GridData)panel.getLayoutData();
//        gd.exclude = !visible;
//        panel.setVisible(visible);
//    }
//
//    private void relayoutAndResize()
//    {
//        Shell shell = getShell();
//        if (shell == null || shell.isDisposed())
//            return;
//
//        shell.setRedraw(false);
//        try
//        {
//            shell.layout(true, true);
//
//            Point cur = shell.getSize();
//            Point computed = shell.computeSize(cur.x, SWT.DEFAULT);
//            shell.setSize(cur.x, computed.y);
//        }
//        finally
//        {
//            shell.setRedraw(true);
//        }
//    }
//}

/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.preferences;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Shell;

import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IWeb;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class TestReportDialog
    extends ErrorDialog
{
    private static final int EXPLORE_ID = IDialogConstants.CANCEL_ID + 10;
    private static final int EXPORT_LOG_ID = IDialogConstants.CANCEL_ID + 11;

    @Inject
    private IWeb web;

    private final String url;
    private final String supportLogText;

    /**
     * @param parentShell
     * @param dialogTitle
     * @param message
     * @param status
     * @param displayMask
     */
    public TestReportDialog(Shell parentShell, String dialogTitle, String message, IStatus status, int displayMask,
        String url, String supportLogText)
    {
        super(parentShell, dialogTitle, message, status, displayMask);
        BaseActivator.injectMembers(this);
        this.url = url;
        this.supportLogText = supportLogText;
    }

    @Override
    protected Control createButtonBar(Composite parent)
    {
        Composite bar = new Composite(parent, SWT.NONE);
        bar.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false));
        GridLayout layout = new GridLayout(1, false);
        layout.marginHeight = 0;
        layout.marginWidth = 0;
        layout.verticalSpacing = 6;
        bar.setLayout(layout);

        // buttons row
        Composite buttonsRow = new Composite(bar, SWT.NONE);
        buttonsRow.setLayoutData(new GridData(SWT.RIGHT, SWT.CENTER, true, false));
        GridLayout buttonsRowLayout = new GridLayout(3, false);
        buttonsRow.setLayout(buttonsRowLayout);

        createButton(buttonsRow, EXPLORE_ID, Messages.TestReportDialog_HowToFix_Open, false);
        createButton(buttonsRow, IDialogConstants.OK_ID, IDialogConstants.OK_LABEL, true);
        createButton(buttonsRow, EXPORT_LOG_ID, Messages.TestReportDialog_ExportLog_Button, false);

        return bar;
    }

    @Override
    protected void buttonPressed(int id)
    {
        if (id == EXPLORE_ID)
        {
            if (url != null && !url.isBlank())
            {
                web.browse(url);
            }
            return;
        }
        if (id == EXPORT_LOG_ID)
        {
            exportSupportLog();
            return;
        }
        super.buttonPressed(id);
    }

    private void exportSupportLog()
    {
        Shell shell = getShell();
        if (shell == null || shell.isDisposed())
            return;

        FileDialog fd = new FileDialog(shell, SWT.SAVE);
        fd.setText(Messages.TestReportDialog_ExportLog_Title);
        fd.setFilterExtensions(new String[] { "*.log" }); //$NON-NLS-1$
        fd.setFilterNames(new String[] { "Log file (*.log)" }); //$NON-NLS-1$
        fd.setOverwrite(true);
        fd.setFileName("diagnostics.log"); //$NON-NLS-1$

        String pathStr = fd.open();
        if (pathStr == null || pathStr.isBlank())
            return;

        try
        {
            Files.writeString(Path.of(pathStr), supportLogText == null ? "" : supportLogText, StandardCharsets.UTF_8); //$NON-NLS-1$
        }
        catch (Exception e)
        {
            MessageDialog.openError(shell, Messages.TestReportDialog_Error,
                Messages.TestReportDialog_ExportLog_Error + e.getMessage());
        }
    }
}
