/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletionException;

import org.eclipse.core.runtime.IProgressMonitor;

import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.TraceScenarioType;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.assistent.model.Session;
import com.e1c.edt.ai.assistent.model.SessionErrorResponse;
import com.e1c.edt.ai.assistent.model.SessionRequest;
import com.e1c.edt.ai.assistent.model.SystemInfo;
import com.e1c.edt.ai.assistent.model.UserParameters;
import com.google.common.base.Stopwatch;

/**
 * @author Bogdan Sushkov
 *
 */
public class SessionDiagnosticTest
    implements IDiagnosticTest
{
    @Override
    public String id()
    {
        return "session-diagnostic-test"; //$NON-NLS-1$
    }

    @Override
    public String title()
    {
        return Messages.SessionDiagnosticTest_Title;
    }

    @Override
    public DiagnosticResult execute(IDiagnosticContext context, IProgressMonitor monitor)
    {
        var settings = context.getSettings();
        var url = settings.getUrl() + "api/v1/create_session"; //$NON-NLS-1$
        Map<String, String> facts = new HashMap<>();
        facts.put("java.home", System.getProperty("java.home")); //$NON-NLS-1$ //$NON-NLS-2$
        facts.put("url", url); //$NON-NLS-1$

        try
        {
            if (context.getTraceScenario().getActive() == TraceScenarioType.SSL_ERROR)
            {
                var sslException = new javax.net.ssl.SSLHandshakeException("Simulated SSL handshake error for testing"); //$NON-NLS-1$
                throw sslException;
            }
            var json = context.getJson();
            var project = context.getProject();
            var environment = context.getEnvironment();
            var versionProvider = context.getVersionProvider();
            var configurationParametersProvider = context.getConfigurationParametersProvider();

            var builder = context.getRequestBuilder().create(url);
            if (builder.isEmpty())
            {
                return DiagnosticResult.error(Messages.SessionDiagnosticTest_TestFailed, ServiceState.NONE, null, null);
            }

            var sessionRequest = new SessionRequest();
            var userParams = settings.getUserParameters();
            sessionRequest.serviceParameters = userParams;
            var userParameters = new UserParameters();
            sessionRequest.userParameters = userParameters;
            var pluginVersion = versionProvider.getPluginVersion();
            if (pluginVersion != null)
            {
                userParameters.pluginVersion = pluginVersion.toString();
            }

            var platformVersion = versionProvider.getPlatformVersion();
            if (platformVersion != null && !platformVersion.isBlank())
            {
                userParameters.edtVersion = platformVersion;
            }

            userParameters.tabWidth = settings.getTabWidth();
            userParameters.codeCompletionLinesCount = settings.getCodeCompletionLinesCount();
            userParameters.codeCompletionPolicy = settings.getCodeCompletionPolicy();
            userParameters.isContinuousCodeCompletion =
                CodeCompletionPolicy.MODERATE.isMeet(userParameters.codeCompletionPolicy);
            userParameters.minRequestDelayMs = settings.getMinRequestDelay().toMillis();
            userParameters.timeoutMs = settings.getTimeout().toMillis();
            userParameters.lineSeparator = settings.getLineSeparator();
            userParameters.language = settings.getLanguage();
            userParameters.configurationParameters =
                configurationParametersProvider.getParameters(project).orElse(null);
            userParameters.globalContext = userParams.globalContext;
            userParameters.experimental = userParams.experimental;

            var systemInfo = new SystemInfo();
            sessionRequest.systemInfo = systemInfo;
            systemInfo.osName = environment.getOSName();
            systemInfo.osVersion = environment.getOSVersion();
            systemInfo.arch = environment.getArch();
            systemInfo.availableProcessors = environment.getAvailableProcessors();
            environment.getProcessorName().ifPresent(val -> systemInfo.processorName = val);
            environment.getTotalPhysicalMemorySize().ifPresent(val -> systemInfo.totalPhysicalMemorySize = val);

            var requestBody = json.serialize(sessionRequest);
            var requestBuilder = builder.get();
            var instanceType = settings.getInstanceType();
            if (instanceType.isPresent())
            {
                requestBuilder = requestBuilder.header("Instance-Type", instanceType.get()); //$NON-NLS-1$
            }

            requestBuilder = requestBuilder.POST(BodyPublishers.ofString(requestBody));
            var request = requestBuilder.build();

            var stopwatch = Stopwatch.createStarted();
            request = context.getHttpLog().request(request, id(), requestBody);

            var client = context.getHttpClientBuilder().create().build();
            HttpResponse<String> response = client.sendAsync(request, BodyHandlers.ofString())
                .orTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .thenApply(
                    resp -> context.getHttpLog().response(resp, id(), stopwatch, true, false))
                .join();

            var body = response.body();
            var status = response.statusCode();
            var session = json.deserialize(body, Session.class);
            if (session.isPresent() && session.get().sessionId != null && status >= 200 && status < 300)
            {
                context.setSessionId(session.get().sessionId);
                return DiagnosticResult.ok(Messages.SessionDiagnosticTest_TestPassed);
            }
            if (!body.isEmpty())
            {
                var error = json.deserialize(body, SessionErrorResponse.class).map(i -> i.errorType).orElse(""); //$NON-NLS-1$
                if (error.equals("invalid_session")) //$NON-NLS-1$
                {
                    status = 403;
                }
            }
            return context.getDiagnosticMapper().map(id(), status, null, facts);
        }
        catch (Exception e)
        {
            var actualError = e instanceof CompletionException ? e.getCause() : e;

            return context.getDiagnosticMapper()
                .map(title(), 0, actualError, facts);
        }
    }
}
