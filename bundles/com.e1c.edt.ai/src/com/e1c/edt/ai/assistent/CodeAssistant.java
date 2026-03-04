/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IEnvironment;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.Observables;
import com.e1c.edt.ai.StatisticsType;
import com.e1c.edt.ai.assistent.model.Completion;
import com.e1c.edt.ai.assistent.model.CompletionRequest;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Provider;

class CodeAssistant
    implements ICodeAssistant
{
    private final IHttpLog log;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clientBuilder;
    private final IJson json;
    private final IResponseStreamProcessor responseStreamProcessor;
    private final Provider<IStatistics> statisticsProvider;
    private final ISettings settings;
    private final IEnvironment environment;
    private final ICompressor compressor;
    private final ISessionCall sessionCall;

    @Inject
    public CodeAssistant(IHttpLog log, IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder, IJson json,
        IResponseStreamProcessor responseStreamProcessor,
        Provider<IStatistics> statisticsProvider, ISettings settings, IEnvironment environment,
        ICompressor compressor, ISessionCall sessionCall)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(responseStreamProcessor);
        Preconditions.checkNotNull(statisticsProvider);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(environment);
        Preconditions.checkNotNull(compressor);
        Preconditions.checkNotNull(sessionCall);
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clientBuilder = clientBuilder;
        this.json = json;
        this.responseStreamProcessor = responseStreamProcessor;
        this.statisticsProvider = statisticsProvider;
        this.settings = settings;
        this.environment = environment;
        this.compressor = compressor;
        this.sessionCall = sessionCall;
    }

    @Override
    public IObservable<Completion> createSource(ProjectId projectId,
        ICompletionRequestProvider completionRequestProvider,
        ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(completionRequestProvider);
        Preconditions.checkNotNull(cancellationToken);
        return Observables.create(observer -> {
            generateText(projectId, completionRequestProvider, observer, cancellationToken);
            return Closeables.Empty;
        });
    }

    private void generateText(ProjectId projectId, ICompletionRequestProvider completionRequestProvider,
        IObserver<Completion> observer,
        ICancellationToken cancellationToken)
    {
        var optionalRequest = requestBuilder.create(settings.getUrl() + "api/v1/complete"); //$NON-NLS-1$
        if (optionalRequest.isEmpty())
        {
            observer.onCompleted();
            return;
        }

        var requestBuilder = optionalRequest.get();
        var statistics = statisticsProvider.get();
        String requestBody;
        BodyPublisher bodyPublisher;
        try (var totalMeasurement = statistics.measureDuration(StatisticsType.TOTAL_DURATUION))
        {
            Optional<CompletionRequest> request;
            try (var measurement = statistics.measureDuration(StatisticsType.CONTEXT_DURATUION))
            {
                request = completionRequestProvider.get(statistics, cancellationToken);
            }

            if (request.isEmpty())
            {
                observer.onCompleted();
                return;
            }

            try (var measurement = statistics.measureDuration(StatisticsType.SERIALIZATION_DURATUION))
            {
                requestBody = json.serialize(request.get());
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
            observer.onCompleted();
            return;
        }

        var freePhysicalMemorySize = environment.getFreePhysicalMemorySize();
        if (freePhysicalMemorySize.isPresent())
        {
            requestBuilder =
                requestBuilder.header("X-Free-Physical-Memory-Size", Long.toString(freePhysicalMemorySize.get())); //$NON-NLS-1$
        }

        for (var statValue : statistics.getValues())
        {
            requestBuilder =
                requestBuilder.header(statValue.getStatisticsType().getHeader(), statValue.getValue());
        }

        var client = clientBuilder.create().build();
        var currentRequestBuilder = requestBuilder;
        var call = sessionCall.call(projectId, cancellationToken, session -> {
        	var httpRequestBuilder = currentRequestBuilder;
            var sessionId = session.flatMap(s -> Optional.ofNullable(s.sessionId)).orElse(null);
        	if (sessionId != null)
        	{
        		httpRequestBuilder = httpRequestBuilder.header("Session-Id", sessionId); //$NON-NLS-1$
        	}

            var request = httpRequestBuilder.POST(bodyPublisher).build();
            log.request(request, cancellationToken.toString(), requestBody);
            return client.sendAsync(request, BodyHandlers.ofLines());
        });

        var stopwatch = Stopwatch.createStarted();
        call.orTimeout(settings.getTimeout().toNanos(), TimeUnit.NANOSECONDS)
            .thenApply(response -> log.response(response, null, stopwatch, true, true))
            .thenApply(response -> checkResponse(response, observer, cancellationToken))
            .thenAccept(response -> {
                var statusCode = response.statusCode();
                if (statusCode >= 200 && statusCode < 300)
                {
                    processStream(response.body(), observer, cancellationToken);
                }
            })
            .whenComplete((r, error) -> {
                if (error != null)
                {
                    observer.onError(error);
                }
            });
    }

    private HttpResponse<Stream<String>> checkResponse(HttpResponse<Stream<String>> response,
        IObserver<Completion> observer, ICancellationToken cancellationToken)
    {
        var statusCode = response.statusCode();
        if (statusCode >= 300)
        {
            observer.onError(
                new AIClientException("AI HTTP response " + cancellationToken + " status code is " + statusCode, null)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        return response;
    }

    private void processStream(Stream<String> stream, IObserver<Completion> observer, ICancellationToken cancellationToken)
    {
        try
        {
            responseStreamProcessor.process(stream, observer, cancellationToken);
        }
        catch (Exception e)
        {
            observer.onError(e);
        }
        finally
        {
            observer.onCompleted();
        }
    }
}