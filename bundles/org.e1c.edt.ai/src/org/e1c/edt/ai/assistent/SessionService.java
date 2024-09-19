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

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.ServerAccessType;
import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.assistent.model.Session;
import org.e1c.edt.ai.assistent.model.SessionRequest;
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
    private final ISettingsProvider settingsProvider;
    private final IServerAccessService serverAccess;

    @Inject
    public SessionService(IHttpLog log, IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder, IJson json,
        ISettingsTracker settingsTracker,
        IResponseCache<Session> responseCache, IParametersService parametersService, ISettingsProvider settingsProvider,
        IServerAccessService serverAccess)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(settingsTracker);
        Preconditions.checkNotNull(responseCache);
        Preconditions.checkNotNull(parametersService);
        Preconditions.checkNotNull(settingsProvider);
        Preconditions.checkNotNull(serverAccess);
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clienBuilder = clientBuilder;
        this.json = json;
        this.settingsTracker = settingsTracker;
        this.responseCache = responseCache;
        this.parametersService = parametersService;
        this.settingsProvider = settingsProvider;
        this.serverAccess = serverAccess;
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

        var settings = settingsProvider.getSettings();
        var reset = settingsTracker.register(SessionService.class.getName(), settings);

        var sessionRequest = new SessionRequest();
        sessionRequest.serviceParameters = parameters.get();

        var requestBody = json.serialize(sessionRequest);
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
                    if (statusCode >= 500)
                    {
                        serverAccess.accessChanged(FeedbackService.class.getName(), ServerAccessType.ACCESS_ABSENT);
                    }
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