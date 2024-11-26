/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

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
import org.e1c.edt.ai.assistent.model.Session;
import org.e1c.edt.ai.client.AIClientException;

import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class CodeAssistant
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

    @Inject
    public CodeAssistant(IHttpLog log,
        IRequestBuilder requestBuilder,
        IHttpClientBuilder clientBuilder, IJson json,
        ISessionService sessionService,
        IResponseStreamProcessor responseStreamProcessor, Provider<IStatistics> statisticsProvider,
        IUISettings uiSettings,
        IEnvironment environment)
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
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clientBuilder = clientBuilder;
        this.json = json;
        this.sessionService = sessionService;
        this.responseStreamProcessor = responseStreamProcessor;
        this.statisticsProvider = statisticsProvider;
        this.uiSettings = uiSettings;
        this.environment = environment;
    }

    @Override
    public IObservable<Completion> createSource(ILocalContextProvider localContextProvider,
        ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(localContextProvider);
        Preconditions.checkNotNull(cancellationToken);
        return Observables.create(observer -> {
            sessionService.getSessionAsync().whenComplete((session, error) -> {
                if (error == null)
                {
                    if (session != null && session.isPresent())
                    {
                        generateText(session.get(), localContextProvider, observer, cancellationToken);
                    }
                    else
                    {
                        observer.onCompleted();
                    }
                }
                else
                {
                    observer.onError(error);
                }
            });

            return Closeables.Empty;
        });
    }

    private void generateText(Session session, ILocalContextProvider localContextProvider,
        IObserver<Completion> observer,
        ICancellationToken cancellationToken)
    {
        var statistics = statisticsProvider.get();
        String requestBody;
        byte[] compressedBody = null;
        try (var totalMeasurement = statistics.measureDuration(StatisticsType.TOTAL_DURATUION))
        {
            var localContext = localContextProvider.get(statistics, cancellationToken);
            var aiRequest = new CompletionRequest();
            aiRequest.localContext = localContext;
            try (var measurement = statistics.measureDuration(StatisticsType.SERIALIZATION_DURATUION))
            {
                requestBody = json.serialize(aiRequest);
            }

            try (var measurement = statistics.measureDuration(StatisticsType.COMPRESSION_DURATUION))
            {
                compressedBody = compress(requestBody).toByteArray();
            }
        }
        catch (Exception error)
        {
            log.error(error, cancellationToken.toString());
            observer.onCompleted();
            return;
        }

        var optionalReauest = requestBuilder.create("./complete"); //$NON-NLS-1$
        if (optionalReauest.isEmpty())
        {
            observer.onCompleted();
            return;
        }

        var requestBuilder = optionalReauest.get()
            .header("Session-Id", session.sessionId) //$NON-NLS-1$
            .header("Content-Encoding", "gzip"); //$NON-NLS-1$ //$NON-NLS-2$

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

        var request = requestBuilder.POST(BodyPublishers.ofByteArray(compressedBody)).build();
        log.request(request, cancellationToken.toString(), requestBody);

        var clien = clientBuilder.create().build();
        var asyncRequest = clien.sendAsync(request, BodyHandlers.ofLines());
        var stopwatch = Stopwatch.createStarted();
        var attachToken = CancellationTokenSource.attach(cancellationToken, () -> asyncRequest.cancel(true));
        asyncRequest
            .orTimeout(uiSettings.getTimeout().toNanos(), TimeUnit.NANOSECONDS)
            .thenApplyAsync(response -> log.response(response, cancellationToken.toString(), stopwatch))
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

                    attachToken.close();
                    observer.onCompleted();
                }
                catch (Exception ex)
                {
                    //
                }
            });
    }

    public static ByteArrayOutputStream compress(String str) throws Exception
    {
        ByteArrayOutputStream obj = new ByteArrayOutputStream();
        try (GZIPOutputStream gzip = new GZIPOutputStream(obj))
        {
            gzip.write(str.getBytes("UTF-8")); //$NON-NLS-1$
            gzip.close();
        }

        return obj;
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
        var attachToken = CancellationTokenSource.attach(cancellationToken, () -> asyncRequest.cancel(true));
        try (attachToken)
        {
            responseStreamProcessor.process(stream, observer, cancellationToken);
        }
        catch (Exception e)
        {
            observer.onError(e);
        }
    }
}