/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.net.ssl.SSLHandshakeException;

import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ServiceState;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
class HealthCheckService
    implements IHealthCheckService
{
    private final IHttpLog log;
    private final ISettings settings;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clientBuilder;

    @Inject
    public HealthCheckService(IHttpLog log, ISettings settings, IRequestBuilder requestBuilder,
        IHttpClientBuilder clientBuilder)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        this.log = log;
        this.settings = settings;
        this.requestBuilder = requestBuilder;
        this.clientBuilder = clientBuilder;
    }

    @Override
    public synchronized CompletableFuture<ServiceState> checkAsync()
    {
        Optional<HttpRequest.Builder> builder = Optional.empty();
        try {
            builder = requestBuilder.create(settings.getUrl() + "api/v1/health"); //$NON-NLS-1$
        } catch (IllegalArgumentException e) {
            log.error(e, "Illegal http request"); //$NON-NLS-1$
            return CompletableFuture.completedFuture(ServiceState.OFFLINE);
        }

        if (builder.isEmpty())
        {
            return CompletableFuture.completedFuture(ServiceState.OFFLINE);
        }

        var request = builder.get().GET().build();
        var stopwatch = Stopwatch.createStarted();
        return clientBuilder.create()
            .build()
            .sendAsync(request, BodyHandlers.ofString())
            .thenApply(response -> log.response(response, null, stopwatch, false, false))
            .thenApply(response -> {
                return response.statusCode() >= 400 ? ServiceState.OFFLINE : ServiceState.ONLINE;
            })
            .exceptionally(throwable -> {
                var cause = throwable.getCause();
                if (cause instanceof SSLHandshakeException)
                {
                    log.error(throwable.getMessage(), "SSL Handshake error "); //$NON-NLS-1$
                    return ServiceState.SSL_ERROR;
                }
                else
                {
                    log.error(throwable.getMessage(), "Error during health check"); //$NON-NLS-1$
                    return ServiceState.OFFLINE;
                }
            });
    }
}
