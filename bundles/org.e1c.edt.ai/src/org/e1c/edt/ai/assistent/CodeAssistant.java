/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

import org.e1c.edt.ai.ActionState;
import org.e1c.edt.ai.CancellationTokenSource;
import org.e1c.edt.ai.Closeables;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.IObservable;
import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.Observables;
import org.e1c.edt.ai.StatisticsType;
import org.e1c.edt.ai.assistent.model.Completion;
import org.e1c.edt.ai.assistent.model.CompletionRequest;
import org.e1c.edt.ai.assistent.model.ProjectId;
import org.e1c.edt.ai.assistent.model.Session;
import org.e1c.edt.ai.client.AIClientException;

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
    private final ISessionService sessionService;
    private final IResponseStreamProcessor responseStreamProcessor;
    private final Provider<IStatistics> statisticsProvider;
    private final IUISettings uiSettings;
    private final IEnvironment environment;
    private final ICompressor compressor;
    private final IStateService stateService;

    @Inject
    public CodeAssistant(IHttpLog log, IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder, IJson json,
        ISessionService sessionService, IResponseStreamProcessor responseStreamProcessor,
        Provider<IStatistics> statisticsProvider, IUISettings uiSettings, IEnvironment environment,
        ICompressor compressor, IStateService stateService)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(sessionService);
        Preconditions.checkNotNull(responseStreamProcessor);
        Preconditions.checkNotNull(statisticsProvider);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(environment);
        Preconditions.checkNotNull(compressor);
        Preconditions.checkNotNull(stateService);
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clientBuilder = clientBuilder;
        this.json = json;
        this.sessionService = sessionService;
        this.responseStreamProcessor = responseStreamProcessor;
        this.statisticsProvider = statisticsProvider;
        this.uiSettings = uiSettings;
        this.environment = environment;
        this.compressor = compressor;
        this.stateService = stateService;
    }

    @Override
    public IObservable<Completion> createSource(ProjectId projectId,
        ICompletionRequestProvider completionRequestProvider,
        ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(completionRequestProvider);
        Preconditions.checkNotNull(cancellationToken);
        return Observables.create(observer -> {
            sessionService.getSessionAsync(projectId).whenComplete((session, error) -> {
                if (error == null)
                {
                    if (session != null && session.isPresent())
                    {
                        generateText(session.get(), completionRequestProvider, observer, cancellationToken);
                    }
                    else
                    {
                        observer.onCompleted();
                    }
                }
                else
                {
                    stateService.setState(CodeAssistant.class.getName(), ActionState.INACTIVE);
                    observer.onError(error);
                }
            });

            return Closeables.Empty;
        });
    }

    private void generateText(Session session, ICompletionRequestProvider сompletionRequestProvider,
        IObserver<Completion> observer,
        ICancellationToken cancellationToken)
    {
        var optionalRequest = requestBuilder.create("./complete"); //$NON-NLS-1$
        if (optionalRequest.isEmpty())
        {
            observer.onCompleted();
            return;
        }

        var requestBuilder = optionalRequest.get().header("Session-Id", session.sessionId); //$NON-NLS-1$
        var statistics = statisticsProvider.get();
        String requestBody;
        BodyPublisher bodyPublisher;
        try (var totalMeasurement = statistics.measureDuration(StatisticsType.TOTAL_DURATUION))
        {
            Optional<CompletionRequest> request;
            try (var measurement = statistics.measureDuration(StatisticsType.CONTEXT_DURATUION))
            {
                request = сompletionRequestProvider.get(statistics, cancellationToken);
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

        var request = requestBuilder.POST(bodyPublisher).build();
        log.request(request, cancellationToken.toString(), requestBody);

        var clien = clientBuilder.create().build();
        var asyncRequest = clien.sendAsync(request, BodyHandlers.ofLines());
        var stopwatch = Stopwatch.createStarted();
        var attachToken = CancellationTokenSource.attach(cancellationToken, () -> {
            stateService.setState(CodeAssistant.class.getName(), ActionState.INACTIVE);
            asyncRequest.cancel(true);
        });

        stateService.setState(CodeAssistant.class.getName(), ActionState.BUSY);
        asyncRequest
            .orTimeout(uiSettings.getTimeout().toNanos(), TimeUnit.NANOSECONDS)
            .thenApplyAsync(response -> log.response(response, cancellationToken.toString(), stopwatch, true))
            .thenApplyAsync(response -> checkResponse(response, observer, cancellationToken))
            .thenApplyAsync(HttpResponse::body)
            .thenAcceptAsync(stream -> processStream(asyncRequest, stream, observer, cancellationToken))
            .whenComplete((r, error) -> {
                try
                {
                    if (!isCancellationException(error))
                    {
                        log.error(error, cancellationToken.toString());
                    }

                    stateService.setState(CodeAssistant.class.getName(), ActionState.INACTIVE);
                    attachToken.close();
                    observer.onCompleted();
                }
                catch (Exception ex)
                {
                    //
                }
            });
    }

    private boolean isCancellationException(Throwable error)
    {
        return error instanceof CompletionException
            && ((CompletionException)error).getCause() instanceof CancellationException;
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

    private void processStream(CompletableFuture<HttpResponse<Stream<String>>> asyncRequest, Stream<String> stream,
        IObserver<Completion> observer, ICancellationToken cancellationToken)
    {
        var attachToken = CancellationTokenSource.attach(cancellationToken, () -> {
            stateService.setState(CodeAssistant.class.getName(), ActionState.INACTIVE);
            asyncRequest.cancel(true);
        });

        try (attachToken)
        {
            responseStreamProcessor.process(stream, observer, cancellationToken);
        }
        catch (Exception e)
        {
            observer.onError(e);
        }

        stateService.setState(CodeAssistant.class.getName(), ActionState.INACTIVE);
    }
}