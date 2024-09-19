/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class CheckStatusService
    implements ICheckStatusService
{
    private final IHttpLog log;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clientBuilder;

    @Inject
    public CheckStatusService(IHttpLog log, IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clientBuilder = clientBuilder;
    }

    @Override
    public synchronized CompletableFuture<Integer> getStatusAsync()
    {
        Optional<HttpRequest.Builder> builder = Optional.empty();
        try {
            builder = requestBuilder.create("./health"); //$NON-NLS-1$
        } catch (IllegalArgumentException e) {
            log.error(e, "Illegal http request"); //$NON-NLS-1$
            return CompletableFuture.completedFuture(0);
        }
        if (builder.isEmpty())
        {
            return CompletableFuture.completedFuture(0);
        }

        var request = builder.get().GET().build();
        var stopwatch = Stopwatch.createStarted();
        return clientBuilder.create()
            .build()
            .sendAsync(request, BodyHandlers.ofString())
            .thenApplyAsync(response -> log.response(response, null, stopwatch))
            .thenApplyAsync(response -> {
                return response.statusCode();
            });
    }
}
