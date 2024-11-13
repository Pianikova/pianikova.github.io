/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.e1c.edt.ai.IEnvironment;
import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.IVersionProvider;
import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.assistent.model.Session;
import org.e1c.edt.ai.assistent.model.SessionRequest;
import org.e1c.edt.ai.assistent.model.SystemInfo;
import org.e1c.edt.ai.assistent.model.UserParameters;
import org.e1c.edt.ai.client.AIClientException;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

public class SessionService implements ISessionService
{
    private final IHttpLog log;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clienBuilder;
    private final IJson json;
    private final ISettingsTracker settingsTracker;
    private final IResponseCache<Session> responseCache;
    private final IParametersService parametersService;
    private final IVersionProvider versionProvider;
    private final IUISettings uiSettings;
    private final IEnvironment environment;

    @Inject
    public SessionService(IHttpLog log, IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder, IJson json,
        ISettingsTracker settingsTracker,
        IResponseCache<Session> responseCache, IParametersService parametersService,
        IVersionProvider versionProvider, IUISettings uiSettings, IEnvironment environment)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(settingsTracker);
        Preconditions.checkNotNull(responseCache);
        Preconditions.checkNotNull(parametersService);
        Preconditions.checkNotNull(versionProvider);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(environment);
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clienBuilder = clientBuilder;
        this.json = json;
        this.settingsTracker = settingsTracker;
        this.responseCache = responseCache;
        this.parametersService = parametersService;
        this.versionProvider = versionProvider;
        this.uiSettings = uiSettings;
        this.environment = environment;
    }

    @Override
    public CompletableFuture<Optional<Session>> getSessionAsync()
    {
        return parametersService.getParametersAsync().thenApplyAsync(parameters -> getSession(parameters).join());
    }

    private CompletableFuture<Optional<Session>> getSession(Optional<Parameters> parameters)
    {
        if (parameters.isEmpty())
        {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        var builder = requestBuilder.create("./create_session"); //$NON-NLS-1$
        if (builder.isEmpty())
        {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        var sessionRequest = new SessionRequest();
        sessionRequest.serviceParameters = parameters.get();
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

        userParameters.tabWidth = uiSettings.getTabWidth();
        userParameters.codeCompletionLinesCount = uiSettings.getCodeCompletionLinesCount();
        userParameters.isContinuousCodeCompletion = uiSettings.isContinuousCodeCompletion();
        userParameters.minRequestDelayMs = uiSettings.getMinRequestDelay().toMillis();
        userParameters.timeoutMs = uiSettings.getTimeout().toMillis();
        userParameters.lineSeparator = uiSettings.getLineSeparator();
        userParameters.sendContext = uiSettings.sendContext();
        userParameters.language = uiSettings.getLanguage();

        var systemInfo = new SystemInfo();
        sessionRequest.systemInfo = systemInfo;
        systemInfo.osName = environment.getOSName();
        systemInfo.osVersion = environment.getOSVersion();
        systemInfo.arch = environment.getArch();
        systemInfo.availableProcessors = environment.getAvailableProcessors();
        environment.getProcessorName().ifPresent(val -> systemInfo.processorName = val);
        environment.getTotalPhysicalMemorySize().ifPresent(val -> systemInfo.totalPhysicalMemorySize = val);

        var requestBody = json.serialize(sessionRequest);
        var reset = settingsTracker.register(SessionService.class.getName(), requestBody);
        var request = builder.get().POST(BodyPublishers.ofString(requestBody)).build();
        return responseCache.get(() -> getSessionAsync(request, requestBody), reset);
    }

    private CompletableFuture<Optional<Session>> getSessionAsync(HttpRequest request, String body)
    {
        log.request(request, null, body);
        var stopwatch = Stopwatch.createStarted();
        return clienBuilder.create()
            .build()
            .sendAsync(request, BodyHandlers.ofString())
            .thenApplyAsync(response -> log.response(response, null, stopwatch))
            .thenApplyAsync(response -> {
                var statusCode = response.statusCode();
                if (statusCode >= 300)
                {
                    throw new AIClientException("AI HTTP session response status code is " + statusCode, null); //$NON-NLS-1$
                }

                return response;
            })
            .thenApplyAsync(HttpResponse::body)
            .thenApplyAsync(this::createCession);
    }

    private Optional<Session> createCession(String content)
    {
        return json.deserialize(content, Session.class);
    }
}