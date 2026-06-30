/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.preferences;

import org.eclipse.osgi.util.NLS;

/**
 * That class was created in order to provide message localization.
 * @author Bogdan Sushkov
 *
 */
public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = "com.e1c.edt.ai.ui.preferences.messages"; //$NON-NLS-1$
    public static String ClientAIPreferencePage_Api_URL;
    public static String ClientAIPreferencePage_Client_Token;
    public static String ClientAIPreferencePage_CodeCompletionLinesCount;
    public static String ClientAIPreferencePage_Language;
    public static String ClientAIPreferencePage_Parameters;

    public static String ClientAIPreferencePage_Language_Default;
    public static String ClientAIPreferencePage_Language_English;
    public static String ClientAIPreferencePage_Language_Russian;

    public static String ClientAIPreferencePage_CodeCompletionPolicy;
    public static String ClientAIPreferencePage_ShowStatusBar;
    public static String ClientAIPreferencePage_AutoOpenDiffPreview;
    public static String ClientAIPreferencePage_CodeCompletionGroup;
    public static String ClientAIPreferencePage_ChatGroup;
    public static String ClientAIPreferencePage_PluginVersion;
    public static String ClientAIPreferencePage_CopyVersion;

    public static String ClientAIPreferencePage_Diagnostic;
    public static String ClientAIPreferencePage_Diagnostic_Title;
    public static String ClientAIPreferencePage_Diagnostic_RunButton;

    public static String Error_UnableToParse;
    public static String Error_OutOfRange;
    public static String Error_Unknown;
    public static String Error_InvalidToken;
    public static String ClientAIPreferencePage_Client_Token_Tooltip;
    public static String TokenFieldEditor_Validate;
    public static String TokenFieldEditor_ValidationError;
    public static String TokenFieldEditor_ValidationSuccess;
    public static String TokenFieldEditor_TokenValid;
    public static String TokenFieldEditor_TokenInvalid;
    public static String ClientAIPreferencePage_CodeCompletionPolicy_Tooltip;
    public static String ClientAIPreferencePage_CodeCompletionLinesCount_Tooltip;
    public static String ClientAIPreferencePage_Language_Tooltip;
    public static String ClientAIPreferencePage_Parameters_Tooltip;

    public static String DiagnosticDialog_CheckingConnection;
    public static String DiagnosticDialog_ExecutionError;
    public static String DiagnosticDialog_ExecutionFailed;
    public static String DiagnosticDialog_Failed;
    public static String DiagnosticDialog_Message;
    public static String DiagnosticDialog_OK;
    public static String DiagnosticDialog_Skip;
    public static String DiagnosticDialog_CloseButton;
    public static String DiagnosticDialog_OpenReport;
    public static String DiagnosticDialog_Preparing;
    public static String DiagnosticDialog_ProblemsDetected;
    public static String DiagnosticDialog_Ready;
    public static String DiagnosticDialog_Successful;
    public static String DiagnosticDialog_Title;
    public static String DiagnosticDialog_TroubleshootingLink;
    public static String DiagnosticReportDialog_EmptyMessage;
    public static String DiagnosticReportDialog_Report;
    public static String DiagnosticReportDialog_Test;
    public static String TestReportDialog_Error;
    public static String TestReportDialog_DiagnosticNotPassed;
    public static String TestReportDialog_ExportLog_Button;
    public static String TestReportDialog_ExportLog_Error;
    public static String TestReportDialog_ExportLog_Title;
    public static String TestReportDialog_HowToFix_Open;
    public static String TestReportDialog_StepsNotPresented;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
