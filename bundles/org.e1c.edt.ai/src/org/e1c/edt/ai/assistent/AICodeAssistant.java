/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.Authenticator;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CancellationTokenSource;
import org.e1c.edt.ai.Closeables;
import org.e1c.edt.ai.CodeCompletionType;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IObservable;
import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.Observables;
import org.e1c.edt.ai.assistent.model.AITextRequest;
import org.e1c.edt.ai.client.AIClientException;
import org.e1c.edt.ai.client.AISettings;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AICodeAssistant
    implements IAICodeAssistant
{
    private final ILog log;
    private final ISettingsProvider settingsProvider;
    private final IJson json;
    private final IResponseStreamProcessor responseStreamProcessor;

    @Inject
    public AICodeAssistant(ILog log, ISettingsProvider settingsProvider,
        IJson json,
        IResponseStreamProcessor responseStreamProcessor)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settingsProvider);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(responseStreamProcessor);
        this.log = log;
        this.settingsProvider = settingsProvider;
        this.json = json;
        this.responseStreamProcessor = responseStreamProcessor;
    }

    @Override
    public IObservable<String> generate(AIContext aiContext,
        ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(aiContext);
        Preconditions.checkNotNull(cancellationToken);
        return Observables.create(observer -> {
            var settings = settingsProvider.getSettings();
            if (settings.isEmpty())
            {
                observer.onCompleted();
                return Closeables.Empty;
            }

            generateText(settings.get(), aiContext, observer, cancellationToken);
            return Closeables.Empty;
        });
    }

    private void generateText(AISettings settings, AIContext aiContext,
        IObserver<String> observer,
        ICancellationToken cancellationToken)
    {
        var aiRequest = new AITextRequest();
        aiRequest.setInputs(aiContext.getContext());
        aiRequest.setParameters(settings.getLlmParameters());
        var requestBody = json.serialize(aiRequest);

        URI uri;
        try
        {
            uri = (aiContext.getComplitionType() == CodeCompletionType.CodeComments
                || aiContext.getComplitionType() == CodeCompletionType.CodeCommentsContinue)
                    ? new URI(String.format("http://gpu22.egom.ailab:8094/generate_stream?client_id=%s&client_uid=%s", //$NON-NLS-1$
                        settings.getClientUniqueId(), settings.getClientToken()))
                    : settings.getApiURL().toURI();
        }
        catch (URISyntaxException e)
        {
            observer.onError(e);
            return;
        }

        HttpRequest request;
        HttpClient client;

        if (aiContext.getComplitionType() == CodeCompletionType.CodeComments)
        {
            request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMinutes(1))
                .header("Accept", "application/json") //$NON-NLS-1$//$NON-NLS-2$
                .header("Content-Type", "application/json") //$NON-NLS-1$//$NON-NLS-2$
                .POST(BodyPublishers.ofString(requestBody))
                .build();

            log.trace("AI request " + cancellationToken, //$NON-NLS-1$
                request.toString() + System.lineSeparator() + requestBody);

            client = HttpClient.newBuilder().version(Version.HTTP_2).authenticator(Authenticator.getDefault()).build();

        }
        else
        {
            request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofMinutes(1))
                .header("Accept", "application/json") //$NON-NLS-1$//$NON-NLS-2$
                .header("Content-Type", "application/json") //$NON-NLS-1$//$NON-NLS-2$
                .header("client_id", settings.getClientToken()) //$NON-NLS-1$
                .header("client_uid", settings.getClientUniqueId()) //$NON-NLS-1$
                .POST(BodyPublishers.ofString(requestBody))
                .build();

            log.trace("AI request " + cancellationToken, //$NON-NLS-1$
                request.toString() + System.lineSeparator() + requestBody);

            client = HttpClient.newBuilder()
                .version(Version.HTTP_2)
                .followRedirects(Redirect.NORMAL)
                .authenticator(Authenticator.getDefault())
                .proxy(ProxySelector.getDefault())
                .build();
        }

        var asyncRequest = client.sendAsync(request, BodyHandlers.ofLines());
        asyncRequest
            .thenApplyAsync(response -> checkResponse(response, observer, cancellationToken))
            .thenApplyAsync(HttpResponse::body)
            .thenAcceptAsync(stream -> processStream(asyncRequest, stream, observer, cancellationToken))
            .whenComplete((r, e) -> observer.onCompleted());
    }

    private HttpResponse<Stream<String>> checkResponse(HttpResponse<Stream<String>> response,
        IObserver<String> observer, ICancellationToken cancellationToken)
    {
        var statusCode = response.statusCode();
        if (statusCode >= 300)
        {
            observer.onError(
                new AIClientException("AI HTTP response " + cancellationToken + " status code is " + statusCode, null)); //$NON-NLS-1$ //$NON-NLS-2$
        }

        log.trace("AI response " + cancellationToken, response.toString()); //$NON-NLS-1$
        return response;
    }

    private void processStream(CompletableFuture<HttpResponse<Stream<String>>> asyncRequest, Stream<String> stream,
        IObserver<String> observer, ICancellationToken cancellationToken)
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