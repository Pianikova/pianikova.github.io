/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.client.AIClientException;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ParametersService
    implements IParametersService
{
    private final IHttpLog log;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clienBuilder;
    private final IJson json;
    private final IResponseCache<Parameters> responseCache;
    private final ISettingsProvider settingsProvider;
    private String lastClientToken;
    private URI lastUri;
    private Parameters lastUserParams;

    @Inject
    public ParametersService(IHttpLog log, IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder,
        IJson json,
        IResponseCache<Parameters> responseCache, ISettingsProvider settingsProvider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(responseCache);
        Preconditions.checkNotNull(settingsProvider);
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clienBuilder = clientBuilder;
        this.json = json;
        this.responseCache = responseCache;
        this.settingsProvider = settingsProvider;
    }

    @Override
    public synchronized CompletableFuture<Optional<Parameters>> getParameters()
    {
        var builder = requestBuilder.create("./params"); //$NON-NLS-1$
        if (builder.isEmpty())
        {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        var request = builder.get().GET().build();
        var newUri = request.uri();
        var newClientToken = settingsProvider.getSettings().map(i -> i.getClientToken()).orElse(""); //$NON-NLS-1$
        var newUserParams = settingsProvider.getSettings().map(i -> i.getLlmParameters()).orElse(new Parameters());
        boolean reset;
        if (newClientToken.equals(lastClientToken) && newUri.equals(lastUri) && newUserParams.equals(lastUserParams))
        {
            reset = false;
        }
        else
        {
            lastClientToken = newClientToken;
            lastUri = newUri;
            lastUserParams = newUserParams;
            reset = true;
        }

        return responseCache.get(() -> getParameters(request, newUserParams), reset);
    }

    private CompletableFuture<Optional<Parameters>> getParameters(HttpRequest request, Parameters userParams)
    {
        log.request(request, null, null);
        return clienBuilder.create()
            .build()
            .sendAsync(request, BodyHandlers.ofString())
            .thenApplyAsync(response -> log.response(response, null))
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
        return json.deserialize(content, Parameters.class)
            .map(params -> params.merge(userParams));
    }
}