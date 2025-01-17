/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.StatisticsType;
import org.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import org.e1c.edt.ai.assistent.model.GlobalContextUpdateResponse;
import org.e1c.edt.ai.assistent.model.ProjectId;
import org.e1c.edt.ai.assistent.model.Session;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

class GlobalContextService
    implements IGlobalContextService
{
    private final IHttpLog log;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clienBuilder;
    private final IJson json;
    private final ISessionService sessionService;
    private final IEnvironment environment;
    private final ICompressor compressor;

    @Inject
    public GlobalContextService(IHttpLog log, IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder,
        IJson json, ISessionService sessionService, IEnvironment environment, ICompressor compressor)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(sessionService);
        Preconditions.checkNotNull(environment);
        Preconditions.checkNotNull(compressor);
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clienBuilder = clientBuilder;
        this.json = json;
        this.sessionService = sessionService;
        this.environment = environment;
        this.compressor = compressor;
    }

    @Override
    public CompletableFuture<Optional<GlobalContextUpdateResponse>> update(ProjectId projectId,
        Collection<GlobalContextUpdate> updates,
        IStatistics statistics, ICancellationToken cancellationToken)
    {
        return sessionService.getSessionAsync(projectId)
            .<Optional<GlobalContextUpdateResponse>> thenApply(session -> {
                if (session.isEmpty())
                {
                    return Optional.empty();
                }

                try
                {
                    return update(session.get(), updates, statistics, cancellationToken).get();
                }
                catch (Exception error)
                {
                    log.error(error, cancellationToken.toString());
                }

                return Optional.empty();
            });
    }

    private CompletableFuture<Optional<GlobalContextUpdateResponse>> update(Session session,
        Collection<GlobalContextUpdate> updates, IStatistics statistics, ICancellationToken cancellationToken)
    {
        var optionalRequest = requestBuilder.create("./context/update"); //$NON-NLS-1$
        if (optionalRequest.isEmpty())
        {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        var requestBuilder = optionalRequest.get().header("Session-Id", session.sessionId); //$NON-NLS-1$
        String requestBody;
        BodyPublisher bodyPublisher;
        try (var totalMeasurement = statistics.measureDuration(StatisticsType.TOTAL_DURATUION))
        {
            try (var measurement = statistics.measureDuration(StatisticsType.SERIALIZATION_DURATUION))
            {
                requestBody = json.serialize(updates);
            }

            try (var measurement = statistics.measureDuration(StatisticsType.COMPRESSION_DURATUION))
            {
                var optionalData = compressor.compress(requestBody);
                if (optionalData.isPresent())
                {
                    try (var data = optionalData.get())
                    {
                        bodyPublisher = BodyPublishers.ofByteArray(data.toByteArray());
                        requestBuilder = requestBuilder.header("Content-Encoding", "gzip"); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                }
                else
                {
                    bodyPublisher = BodyPublishers.ofString(requestBody);
                }
            }
        }
        catch (Exception error)
        {
            log.error(error, cancellationToken.toString());
            return CompletableFuture.completedFuture(Optional.empty());
        }

        var freePhysicalMemorySize = environment.getFreePhysicalMemorySize();
        if (freePhysicalMemorySize.isPresent())
        {
            requestBuilder =
                requestBuilder.header("X-Free-Physical-Memory-Size", Long.toString(freePhysicalMemorySize.get())); //$NON-NLS-1$
        }

        for (var statValue : statistics.getValues())
        {
            requestBuilder = requestBuilder.header(statValue.getStatisticsType().getHeader(), statValue.getValue());
        }

        var request = requestBuilder.POST(bodyPublisher).build();
        log.request(request, cancellationToken.toString(), requestBody);
        var stopwatch = Stopwatch.createStarted();
        return clienBuilder.create()
            .build()
            .sendAsync(request, BodyHandlers.ofString())
            .thenApplyAsync(response -> log.response(response, null, stopwatch))
            .thenApplyAsync(HttpResponse::body)
            .thenApplyAsync(content -> json.deserialize(content, GlobalContextUpdateResponse.class));
    }
}