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
import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.assistent.model.ParametersReponse;
import org.e1c.edt.ai.client.AIClientException;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

class ParametersService
    implements IParametersService
{
    private final IHttpLog log;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clienBuilder;
    private final IJson json;
    private final ISettingsTracker settingsTracker;
    private final IResponseCache<Parameters> responseCache;
    private final ISettingsProvider settingsProvider;

    @Inject
    public ParametersService(IHttpLog log, IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder,
        IJson json,
        ISettingsTracker settingsTracker,
        IResponseCache<Parameters> responseCache, ISettingsProvider settingsProvider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(settingsTracker);
        Preconditions.checkNotNull(responseCache);
        Preconditions.checkNotNull(settingsProvider);
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clienBuilder = clientBuilder;
        this.json = json;
        this.settingsTracker = settingsTracker;
        this.responseCache = responseCache;
        this.settingsProvider = settingsProvider;
    }

    @Override
    public synchronized CompletableFuture<Optional<Parameters>> getParametersAsync()
    {
        var settings = settingsProvider.getSettings();
        var reset = settingsTracker.register(ParametersService.class.getName(), settings);
        return responseCache.get("", () -> { //$NON-NLS-1$
            var builder = requestBuilder.create("./params"); //$NON-NLS-1$
            if (builder.isEmpty())
            {
                return CompletableFuture.completedFuture(Optional.empty());
            }

            var request = builder.get().GET().build();
            return getParametersAsync(request, settings.getLlmParameters());
        }, reset);
    }

    private CompletableFuture<Optional<Parameters>> getParametersAsync(HttpRequest request, Parameters userParams)
    {
        log.request(request, null, null);
        var stopwatch = Stopwatch.createStarted();
        return clienBuilder.create()
            .build()
            .sendAsync(request, BodyHandlers.ofString())
            .thenApplyAsync(response -> log.response(response, null, stopwatch, true))
            .thenApplyAsync(response -> {
                var statusCode = response.statusCode();
                if (statusCode >= 300)
                {
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