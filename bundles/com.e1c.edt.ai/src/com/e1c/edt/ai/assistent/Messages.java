/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import org.eclipse.osgi.util.NLS;

public class Messages
    extends NLS
{
    private static final String BUNDLE_NAME = Messages.class.getPackageName() + ".messages"; //$NON-NLS-1$
    public static String DiagnosticMapper_401;
    public static String DiagnosticMapper_401_Remediation;
    public static String DiagnosticMapper_403;
    public static String DiagnosticMapper_403_Remediation;
    public static String DiagnosticMapper_407;
    public static String DiagnosticMapper_407_Remediation;
    public static String DiagnosticMapper_500;
    public static String DiagnosticMapper_500_Remediation;
    public static String DiagnosticMapper_Unknown;
    public static String DiagnosticMapper_Unknown_Remediation;
    public static String DiagnosticMapper_IfNotWorking;
    public static String DiagnosticMapper_PKIX;
    public static String DiagnosticMapper_PKIX_Remediation;
    public static String DiagnosticMapper_Timeout;
    public static String DiagnosticMapper_Timeout_Remediation;
    public static String DiagnosticMapper_DNS;
    public static String DiagnosticMapper_DNS_Remediation;
    public static String UpdatingServerStatus;
    public static String ChatDiagnosticTest_ChatWebUiLoaded;
    public static String ChatDiagnosticTest_ChatApiAvailable;
    public static String ChatDiagnosticTest_ExecutedSuccessfully;
    public static String ChatDiagnosticTest_SessionTestPassed;
    public static String ChatDiagnosticTest_Title;
    public static String CodeCompletionDiagnosticTest_SessionIdEmpty;
    public static String CodeCompletionDiagnosticTest_TestFailed;
    public static String CodeCompletionDiagnosticTest_TestPassed;
    public static String CodeCompletionDiagnosticTest_Title;
    public static String HealthCheckDiagnosticTest_TestFailed;
    public static String DiagnosticTest_TestFailed_Remediation;
    public static String HealthCheckDiagnosticTest_TestPassed;
    public static String HealthCheckDiagnosticTest_Title;
    public static String SessionDiagnosticTest_TestPassed;
    public static String SessionDiagnosticTest_TestFailed;
    public static String SessionDiagnosticTest_Title;

    public static String TokenDiagnosticTest_Error;
    public static String TokenDiagnosticTest_NonValid;
    public static String TokenDiagnosticTest_Title;
    public static String TokenDiagnosticTest_Valid;

    static
    {
        // initialize resource bundle
        NLS.initializeMessages(BUNDLE_NAME, Messages.class);
    }

    private Messages()
    {
    }
}
