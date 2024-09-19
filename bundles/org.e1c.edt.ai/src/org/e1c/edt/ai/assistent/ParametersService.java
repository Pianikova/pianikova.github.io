/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.ServerAccessType;
import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.assistent.model.ParametersReponse;
import org.e1c.edt.ai.client.AIClientException;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

public class ParametersService
    implements IParametersService
{
    private final IHttpLog log;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clienBuilder;
    private final IJson json;
    private final ISettingsTracker settingsTracker;
    private final IResponseCache<Parameters> responseCache;
    private final IServerAccessService serverAccess;
    private final ISettingsProvider settingsProvider;

    @Inject
    public ParametersService(IHttpLog log, IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder,
        IJson json,
        ISettingsTracker settingsTracker,
        IResponseCache<Parameters> responseCache, ISettingsProvider settingsProvider, IServerAccessService serverAccess)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(settingsTracker);
        Preconditions.checkNotNull(responseCache);
        Preconditions.checkNotNull(settingsProvider);
        Preconditions.checkNotNull(serverAccess);
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clienBuilder = clientBuilder;
        this.json = json;
        this.settingsTracker = settingsTracker;
        this.responseCache = responseCache;
        this.serverAccess = serverAccess;
        this.settingsProvider = settingsProvider;
    }

    @Override
    public synchronized CompletableFuture<Optional<Parameters>> getParametersAsync()
    {
        var builder = requestBuilder.create("./params"); //$NON-NLS-1$
        if (builder.isEmpty())
        {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        var request = builder.get().GET().build();
        var settings = settingsProvider.getSettings();
        var reset = settingsTracker.register(ParametersService.class.getName(), settings);
        var params = settingsProvider.getSettings().map(i -> i.getLlmParameters()).orElse(new Parameters());
        return responseCache.get(() -> getParametersAsync(request, params), reset);
    }

    private CompletableFuture<Optional<Parameters>> getParametersAsync(HttpRequest request, Parameters userParams)
    {
        log.request(request, null, null);
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
                    throw new AIClientException("AI HTTP parameters response status code is " + statusCode, null); //$NON-NLS-1$
                }

                return response;
            })
            .thenApplyAsync(HttpResponse::body)
            .thenApplyAsync(content -> createParameters(content, userParams));
    }

    private Optional<Parameters> createParameters(String content, Parameters userParams)
    {
        return json.deserialize(content, ParametersReponse.class)
            .map(response -> response.serviceParameters.merge(userParams));
    }
}