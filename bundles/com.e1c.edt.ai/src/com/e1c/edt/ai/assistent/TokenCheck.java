/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IVersionProvider;
import com.e1c.edt.ai.assistent.model.Session;
import com.e1c.edt.ai.assistent.model.SessionRequest;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class TokenCheck
    implements ITokenCheck
{
    private final IJson json;
    private final ISettings settings;
    private final IVersionProvider versionProvider;
    private final IHttpClientBuilder clientBuilder;

    @Inject
    public TokenCheck(IJson json, ISettings settings, IVersionProvider versionProvider,
        IHttpClientBuilder clientBuilder)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(versionProvider);
        Preconditions.checkNotNull(clientBuilder);
        this.json = json;
        this.settings = settings;
        this.versionProvider = versionProvider;
        this.clientBuilder = clientBuilder;
    }

    @Override
    public CompletableFuture<Boolean> checkTokenAsync(String token)
    {
        var uriStr = settings.getUrl() + "api/v1/create_session"; //$NON-NLS-1$
        URI uri;
        try
        {
            uri = new URI(uriStr);
        }
        catch (URISyntaxException e)
        {
            return CompletableFuture.completedFuture(false);
        }

        var sessionRequest = new SessionRequest();
        var userParams = settings.getUserParameters();
        sessionRequest.serviceParameters = userParams;

        var requestBody = json.serialize(sessionRequest);
        var requestBuilder = HttpRequest.newBuilder()
            .uri(uri)
            .timeout(settings.getTimeout())
            .header("Accept", "application/json") //$NON-NLS-1$//$NON-NLS-2$
            .header("Content-Type", "application/json") //$NON-NLS-1$//$NON-NLS-2$
            .header("Unique-Id", settings.getClientUniqueId()) //$NON-NLS-1$
            .header("Authorization", token); //$NON-NLS-1$

        var pluginVersion = versionProvider.getPluginVersion();
        if (pluginVersion != null)
        {
            requestBuilder.header("plugin_version", pluginVersion.toString()); //$NON-NLS-1$
        }

        var instanceType = settings.getInstanceType();
        if (instanceType.isPresent())
        {
            requestBuilder.header("Instance-Type", instanceType.get()); //$NON-NLS-1$
        }

        var request = requestBuilder.POST(BodyPublishers.ofString(requestBody)).build();
        var client = clientBuilder.get();
        return client.sendAsync(request, BodyHandlers.ofString())
            .thenApply(HttpResponse::body)
            .thenApply(content -> checkResponse(content))
            .exceptionally(e -> false);
    }

    private boolean checkResponse(String content)
    {
        try
        {
            var sessionOpt = json.deserialize(content, Session.class);
            if (sessionOpt.isEmpty())
            {
                return false;
            }
            var session = sessionOpt.get();
            return session.sessionId != null && !session.sessionId.isEmpty();
        }
        catch (Exception e)
        {
            return false;
        }
    }
}
