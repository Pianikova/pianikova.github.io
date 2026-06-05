/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.model.CompletionRequest;

/**
 * @author Bogdan Sushkov
 *
 */
public class CodeCompletionDiagnosticTest
    implements IDiagnosticTest
{

    @Override
    public String id()
    {
        return "codecompletion-diagnostic-test"; //$NON-NLS-1$
    }

    @Override
    public String title()
    {
        return Messages.CodeCompletionDiagnosticTest_Title;
    }

    @Override
    public DiagnosticResult execute(IDiagnosticContext context, IProgressMonitor monitor)
    {
        var sessionId = context.getSessionId();
        if (sessionId == null)
        {
            return DiagnosticResult.error(Messages.CodeCompletionDiagnosticTest_SessionIdEmpty, ServiceState.OFFLINE,
                null, null);
        }
        var dummyLocalContext =
            context.getLocalContext().create(context.getAIContext(), context.getStatistics(), CancellationTokens.NONE);
        CompletionRequest request = new CompletionRequest();
        request.localContext = dummyLocalContext;
        var requestBody = context.getJson().serialize(request);
        var bodyPublisher = BodyPublishers.ofString(requestBody);
        var client = context.getHttpClientBuilder().get();

        // trying to call the code completion API endpoint
        var url = context.getSettings().getUrl() + "api/v1/complete"; //$NON-NLS-1$
        DiagnosticResult result;
        var builderOpt = context.getRequestBuilder().create(url);
        if (builderOpt.isEmpty())
        {
            result = DiagnosticResult.error(Messages.CodeCompletionDiagnosticTest_TestFailed,
                ServiceState.OFFLINE, null, null);
        }
        var builder = builderOpt.get();

        var requestInner = builder.header("Session-Id", sessionId).POST(bodyPublisher).build(); //$NON-NLS-1$
        result = client.sendAsync(requestInner, BodyHandlers.ofLines())
            .orTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .handle((response, throwable) -> {
                final var status = response.statusCode();
                if (throwable == null && status >= 200 && status < 300)
                {
                    return DiagnosticResult.ok(Messages.CodeCompletionDiagnosticTest_TestPassed);
                }
                Map<String, String> map = new HashMap<>();
                map.put("java.home", System.getProperty("java.home")); //$NON-NLS-1$ //$NON-NLS-2$
                map.put("url", url); //$NON-NLS-1$
                return context.getDiagnosticMapper()
                    .map(title(), response.statusCode(), throwable,
                        Map.of("java.home", System.getProperty("java.home"))); //$NON-NLS-1$ //$NON-NLS-2$
            })
            .join();

        return result;
    }

}