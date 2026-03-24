/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import com.e1c.edt.ai.ServiceState;

/**
 * @author Bogdan Sushkov
 *
 */
public class HealthCheckDiagnosticTest
    implements IDiagnosticTest
{

    @Override
    public String id()
    {
        return "healthcheck-diagnostic-test"; //$NON-NLS-1$
    }

    @Override
    public String title()
    {
        return Messages.HealthCheckDiagnosticTest_Title;
    }

    @Override
    public DiagnosticResult execute(IDiagnosticContext context, IProgressMonitor monitor)
    {
        try
        {
            var url = context.getSettings().getUrl() + "api/v1/health"; //$NON-NLS-1$
            var builderOpt = context.getRequestBuilder().create(url);
            if (builderOpt.isEmpty())
            {
                return DiagnosticResult.error(Messages.HealthCheckDiagnosticTest_TestFailed,
                    ServiceState.OFFLINE, null, null);
            }
            var builder = builderOpt.get();

            var client = context.getHttpClientBuilder().create().build();
            var request = builder.GET().build();

            request = context.getHttpLog().request(request, id(), null);

            return client.sendAsync(request, java.net.http.HttpResponse.BodyHandlers.ofString())
                .orTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .handle((response, throwable) -> {
                    final var status = response == null ? 500 : response.statusCode();
                    if (throwable == null && status >= 200 && status < 300)
                    {
                        return DiagnosticResult.ok(Messages.HealthCheckDiagnosticTest_TestPassed);
                    }

                    Map<String, String> facts = new java.util.HashMap<>();
                    facts.put("java.home", System.getProperty("java.home")); //$NON-NLS-1$ //$NON-NLS-2$
                    facts.put("url", url); //$NON-NLS-1$

                    return context.getDiagnosticMapper()
                        .map(title(), response.statusCode(), throwable, facts);
                })
                .join();
        }
        finally
        {
            context.setCaReportIfAbsent(context.getCaCertificateReporter().buildPlainLog());
        }
    }

}
