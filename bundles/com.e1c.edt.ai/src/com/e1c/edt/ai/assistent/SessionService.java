/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.IConfigurationParametersProvider;
import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsSetter;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.Session;
import com.e1c.edt.ai.assistent.model.SessionRequest;
import com.e1c.edt.ai.assistent.model.SystemInfo;
import com.e1c.edt.ai.assistent.model.UserParameters;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

class SessionService
    implements ISessionService
{
    private final IHttpLog log;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clientBuilder;
    private final IJson json;
    private final ISettingsTracker settingsTracker;
    private final IResponseCache responseCache;
    private final IVersionProvider versionProvider;
    private final ISettings settings;
    private final ISettingsSetter settingsSetter;
    private final IEnvironment environment;
    private final IConfigurationParametersProvider configurationParametersProvider;
    private final IStateService stateService;

    @Inject
    public SessionService(IHttpLog log, IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder, IJson json,
        ISettingsTracker settingsTracker, IResponseCache responseCache, IVersionProvider versionProvider,
        ISettings settings, ISettingsSetter settingsSetter, IEnvironment environment,
        IConfigurationParametersProvider configurationParametersProvider, IStateService stateService)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(settingsTracker);
        Preconditions.checkNotNull(responseCache);
        Preconditions.checkNotNull(versionProvider);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(settingsSetter);
        Preconditions.checkNotNull(environment);
        Preconditions.checkNotNull(configurationParametersProvider);
        Preconditions.checkNotNull(stateService);
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clientBuilder = clientBuilder;
        this.json = json;
        this.settingsTracker = settingsTracker;
        this.responseCache = responseCache;
        this.versionProvider = versionProvider;
        this.settings = settings;
        this.settingsSetter = settingsSetter;
        this.environment = environment;
        this.configurationParametersProvider = configurationParametersProvider;
        this.stateService = stateService;
    }

    @Override
    public CompletableFuture<Optional<Session>> getSessionAsync(ProjectId projectId)
    {
        var reset = settingsTracker.register(SessionService.class.getName(), settings.getUserParameters());
        return responseCache.get(projectId, () -> getSession(projectId), reset, true);
    }

    private CompletableFuture<Optional<Session>> getSession(ProjectId projectId)
    {
        var builder = requestBuilder.create(settings.getUrl() + "api/v1/create_session"); //$NON-NLS-1$
        if (builder.isEmpty())
        {
            return CompletableFuture.completedFuture(Optional.empty());
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
        userParameters.configurationParameters = configurationParametersProvider.getParameters(projectId).orElse(null);
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
        return getSessionAsync(projectId, requestBuilder.build(), requestBody);
    }

    private CompletableFuture<Optional<Session>> getSessionAsync(ProjectId projectId, HttpRequest request, String body)
    {
        log.request(request, null, body);
        var stopwatch = Stopwatch.createStarted();
        var busyToken = stateService.busy();
        return clientBuilder.create()
            .build()
            .sendAsync(request, BodyHandlers.ofString())
            .thenApply(response -> log.response(response, null, stopwatch, true, true))
            .thenApply(HttpResponse::body)
            .thenApply(content -> createCession(projectId, content))
            .whenComplete((session, error) -> {
                try
                {
                    busyToken.close();
                }
                catch (Exception e)
                {
                    //
                }
            });
    }

    private Optional<Session> createCession(ProjectId projectId, String content)
    {
        return json.deserialize(content, Session.class).map(response -> {
            settingsSetter.applySessionParameters(projectId, response.userParameters);
            return response;
        });
    }
}