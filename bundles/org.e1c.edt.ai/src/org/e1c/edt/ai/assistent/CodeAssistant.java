/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.io.ByteArrayOutputStream;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Locale;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import java.util.zip.GZIPOutputStream;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CancellationTokenSource;
import org.e1c.edt.ai.CancellationTokens;
import org.e1c.edt.ai.Closeables;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IClock;
import org.e1c.edt.ai.IContextEntities;
import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.IObservable;
import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.ITextNormilizer;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.Observables;
import org.e1c.edt.ai.StatisticsType;
import org.e1c.edt.ai.assistent.model.Completion;
import org.e1c.edt.ai.assistent.model.CompletionRequest;
import org.e1c.edt.ai.assistent.model.LocalContext;
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
    private final IContextEntities contextEntities;
    private final ITextNormilizer textNormilizer;
    private final Provider<IStatistics> statisticsProvider;
    private final IClock clock;
    private final IUISettings uiSettings;

    @Inject
    public CodeAssistant(IHttpLog log,
        IRequestBuilder requestBuilder,
        IHttpClientBuilder clientBuilder, IJson json,
        ISessionService sessionService,
        IResponseStreamProcessor responseStreamProcessor, IContextEntities contextEntities,
        ITextNormilizer textNormilizer, Provider<IStatistics> statisticsProvider, IClock clock, IUISettings uiSettings)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(sessionService);
        Preconditions.checkNotNull(responseStreamProcessor);
        Preconditions.checkNotNull(contextEntities);
        Preconditions.checkNotNull(textNormilizer);
        Preconditions.checkNotNull(statisticsProvider);
        Preconditions.checkNotNull(clock);
        Preconditions.checkNotNull(uiSettings);
        this.log = log;
        this.requestBuilder = requestBuilder;
        this.clientBuilder = clientBuilder;
        this.json = json;
        this.sessionService = sessionService;
        this.responseStreamProcessor = responseStreamProcessor;
        this.contextEntities = contextEntities;
        this.textNormilizer = textNormilizer;
        this.statisticsProvider = statisticsProvider;
        this.clock = clock;
        this.uiSettings = uiSettings;
    }

    @Override
    public IObservable<Completion> createSource(AIContext aiContext,
        ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(aiContext);
        Preconditions.checkNotNull(cancellationToken);
        return Observables.create(observer -> {
            sessionService.getSessionAsync().whenComplete((session, error) -> {
                if (error == null)
                {
                    if (session != null && session.isPresent())
                    {
                        generateText(session.get(), aiContext, observer, cancellationToken);
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

    private void generateText(Session session, AIContext aiContext,
        IObserver<Completion> observer,
        ICancellationToken cancellationToken)
    {
        var statistics = statisticsProvider.get();
        String requestBody;
        byte[] compressedBody = null;
        try (var totalMeasurement = statistics.measureDuration(StatisticsType.TOTAL))
        {
            var localContext = new LocalContext();
            localContext.prefix = textNormilizer.normalize(aiContext.getPrefix());
            localContext.suffix = textNormilizer.normalize(aiContext.getSufix());
            localContext.path = aiContext.getPath();
            localContext.offset = aiContext.getSourceOffset();
            try (var measurement = statistics.measureDuration(StatisticsType.CONTEXT))
            {
                var expirationDate = clock.now().plus(uiSettings.getMinRequestDelay());
                var expiringCancellationToken = CancellationTokens.expiresAt(cancellationToken, clock, expirationDate);
                contextEntities.fill(aiContext, localContext, statistics, expiringCancellationToken);
            }

            var aiRequest = new CompletionRequest();
            aiRequest.localContext = localContext;
            try (var measurement = statistics.measureDuration(StatisticsType.SERIALIZATION))
            {
                requestBody = json.serialize(aiRequest);
            }

            try (var measurement = statistics.measureDuration(StatisticsType.COMPRESSION))
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

        for (var statisticsData : statistics.get())
        {
            var durationSeconds = statisticsData.getDuration().toNanos() / 1000000000d;
            requestBuilder =
                requestBuilder.header(statisticsData.getStatisticsType().getHeader(),
                    String.format(Locale.US, "%.9f", durationSeconds)); //$NON-NLS-1$
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