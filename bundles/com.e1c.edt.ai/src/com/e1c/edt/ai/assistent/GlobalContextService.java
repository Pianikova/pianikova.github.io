/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.Collections;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.StatisticsType;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdateResponse;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.Session;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

class GlobalContextService
    implements IGlobalContextService
{
    private final IHttpLog log;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clientBuilder;
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
        this.clientBuilder = clientBuilder;
        this.json = json;
        this.sessionService = sessionService;
        this.environment = environment;
        this.compressor = compressor;
    }

    @Override
    public CompletableFuture<Optional<GlobalContextUpdateResponse>> update(ProjectId projectId,
        Collection<GlobalContextUpdate> updates,
        int partitionSize,
        IStatistics statistics, ICancellationToken cancellationToken)
    {
        return sessionService.getSessionAsync(projectId).<Optional<GlobalContextUpdateResponse>> thenApply(session -> {
            if (session.isEmpty())
            {
                return Optional.empty();
            }

            try
            {
                return update(session.get(), updates, partitionSize, statistics, cancellationToken).get();
            }
            catch (Exception error)
            {
                log.error(error, cancellationToken.toString());
            }

            return Optional.empty();
            });
    }

    private CompletableFuture<Optional<GlobalContextUpdateResponse>> update(Session session,
        Collection<GlobalContextUpdate> updates, int partitionSize, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        CompletableFuture<Optional<GlobalContextUpdateResponse>> feature =
            CompletableFuture.completedFuture(Optional.empty());

        for (var updatePart : Collections.split(updates, getPartitionSize(updates, partitionSize)))
        {
            feature = feature.thenCompose(
                results -> update(results, session, updatePart, statistics, cancellationToken));
        }

        return feature;
    }

    private int getPartitionSize(Collection<GlobalContextUpdate> updates, int defaultPartitionSize)
    {
        if (defaultPartitionSize > 0)
        {
            return defaultPartitionSize;
        }

        var partitionSize = 200;
        for (var update : updates)
        {
            if (update.value != null)
            {
                partitionSize = 10;
                break;
            }
        }

        return partitionSize;
    }

    private CompletableFuture<Optional<GlobalContextUpdateResponse>> update(
        Optional<GlobalContextUpdateResponse> results, Session session,
        Collection<GlobalContextUpdate> updates, IStatistics statistics, ICancellationToken cancellationToken)
    {
        var optionalRequest =
            requestBuilder.create(settings -> settings.getLlmParameters().url, "./api/v1/context/update"); //$NON-NLS-1$
        if (optionalRequest.isEmpty())
        {
            return CompletableFuture.completedFuture(results);
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
            return CompletableFuture.completedFuture(results);
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
        return clientBuilder.create()
            .build()
            .sendAsync(request, BodyHandlers.ofString())
            .thenApplyAsync(response -> log.response(response, null, stopwatch, true))
            .thenApplyAsync(HttpResponse::body)
            .thenApplyAsync(
                content -> combine(results, init(json.deserialize(content, GlobalContextUpdateResponse.class))));
    }

    private Optional<GlobalContextUpdateResponse> init(Optional<GlobalContextUpdateResponse> response)
    {
        if (response.isEmpty())
        {
            return response;
        }

        var resp = response.get();
        if (resp.isEmpty())
        {
            return Optional.empty();
        }

        if (resp.unknownKeys == null)
        {
            resp.unknownKeys = new ArrayList<>();
        }

        if (resp.unknownValues == null)
        {
            resp.unknownValues = new ArrayList<>();
        }

        return Optional.of(resp);
    }

    private Optional<GlobalContextUpdateResponse> combine(Optional<GlobalContextUpdateResponse> results,
        Optional<GlobalContextUpdateResponse> newResults)
    {
        if (results.isEmpty())
        {
            return newResults;
        }

        if (newResults.isEmpty())
        {
            return results;
        }

        var response = new GlobalContextUpdateResponse();
        response.unknownValues = new ArrayList<>();
        response.unknownValues.addAll(results.get().unknownValues);
        response.unknownValues.addAll(newResults.get().unknownValues);
        response.unknownKeys = new ArrayList<>();
        response.unknownKeys.addAll(results.get().unknownKeys);
        response.unknownKeys.addAll(newResults.get().unknownKeys);
        return Optional.of(response);
    }

}