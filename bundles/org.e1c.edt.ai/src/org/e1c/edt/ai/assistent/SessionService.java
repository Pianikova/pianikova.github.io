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
import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.assistent.model.Session;
import org.e1c.edt.ai.assistent.model.SessionRequest;
import org.e1c.edt.ai.client.AIClientException;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class SessionService implements ISessionService
{
    private final IHttpLog log;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clienBuilder;
    private final IJson json;
    private final IResponseCache<Session> responseCache;
    private final IParametersService parametersService;
    private String lastRequestBody;

    @Inject
    public SessionService(IHttpLog log, IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder, IJson json,
        IResponseCache<Session> responseCache, IParametersService parametersService)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(responseCache);
        Preconditions.checkNotNull(parametersService);
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clienBuilder = clientBuilder;
        this.json = json;
        this.responseCache = responseCache;
        this.parametersService = parametersService;
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
        var newRequestBody = json.serialize(sessionRequest);
        var request = builder.get().POST(BodyPublishers.ofString(newRequestBody)).build();
        boolean reset;
        if (newRequestBody.equals(lastRequestBody))
        {
            reset = false;
        }
        else
        {
            lastRequestBody = newRequestBody;
            reset = true;
        }

        return responseCache.get(() -> getSessionAsync(request, newRequestBody), reset);
    }

    private CompletableFuture<Optional<Session>> getSessionAsync(HttpRequest request, String body)
    {
        log.request(request, null, body);
        return clienBuilder.create()
            .build()
            .sendAsync(request, BodyHandlers.ofString())
            .thenApplyAsync(response -> log.response(response, null))
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