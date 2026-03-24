/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import org.eclipse.core.runtime.IProgressMonitor;

import com.e1c.edt.ai.ServiceState;

/**
 * @author Bogdan Sushkov
 *
 */
public class TokenDiagnosticTest
    implements IDiagnosticTest
{

    @Override
    public String id()
    {
        return "token-validity-diagnostic-test"; //$NON-NLS-1$
    }

    @Override
    public String title()
    {
        return Messages.TokenDiagnosticTest_Title;
    }

    @Override
    public DiagnosticResult execute(IDiagnosticContext context, IProgressMonitor monitor)
    {
        String token = context.getSettings().getClientToken();
        return context.getTokenCheck()
            .checkTokenAsync(token)
            .orTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .handle((isValid, throwable) -> {
                if (throwable != null)
                {
                    return DiagnosticResult.error(Messages.TokenDiagnosticTest_Error, ServiceState.OFFLINE, null,
                        throwable);
                }
                if (isValid)
                {
                    return DiagnosticResult.ok(Messages.TokenDiagnosticTest_Valid);
                } else {
                    return DiagnosticResult.error(Messages.TokenDiagnosticTest_NonValid, ServiceState.TOKEN_ERROR,
                        null, null);
                }
            })
            .join();



    }

}
