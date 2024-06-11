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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CancellationToken;
import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IObserver;
import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.assistent.model.AITextRequest;
import org.e1c.edt.ai.client.AIClientException;
import org.e1c.edt.ai.client.AISettings;

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
        this.log = log;
        this.settingsProvider = settingsProvider;
        this.json = json;
        this.responseStreamProcessor = responseStreamProcessor;
    }

    @Override
    public Optional<CompletableFuture<Void>> generate(AIContext aiContext, IObserver<String> observer,
        CancellationToken cancellationToken)
    {
        return settingsProvider.getSettings()
            .flatMap(settings -> generateText(settings, aiContext, observer, cancellationToken));
    }

    private Optional<CompletableFuture<Void>> generateText(AISettings settings, AIContext aiContext,
        IObserver<String> observer,
        CancellationToken cancellationToken)
    {
        var aiRequest = new AITextRequest();
        aiRequest.setInputs(aiContext.getContext());
        aiRequest.setParameters(settings.getLlmParameters());
        var requestBody = json.serialize(aiRequest);

        URI uri;
        try
        {
            uri = settings.getApiURL().toURI();
        }
        catch (URISyntaxException e)
        {
            observer.onError(e);
            return Optional.empty();
        }

        var request = HttpRequest.newBuilder()
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

        var client = HttpClient.newBuilder()
            .version(Version.HTTP_2)
            .followRedirects(Redirect.NORMAL)
            .authenticator(Authenticator.getDefault())
            .proxy(ProxySelector.getDefault())
            .build();

        var asyncRequest = client.sendAsync(request, BodyHandlers.ofLines());
        var feature = asyncRequest
            .thenApplyAsync(rsp -> checkResponse(rsp, observer, cancellationToken))
            .thenApplyAsync(HttpResponse::body)
            .thenAcceptAsync(stream -> {
                var attachToken = cancellationToken.attach(() -> asyncRequest.cancel(true));
                try (attachToken)
                {
                    responseStreamProcessor.process(stream, observer, cancellationToken);
                }
                catch (Exception e)
                {
                    observer.onError(e);
                }
            })
             .exceptionally(e -> {
                 observer.onError(e);
                 return null;
             });

        return Optional.of(feature);
    }

    private HttpResponse<Stream<String>> checkResponse(HttpResponse<Stream<String>> response,
        IObserver<String> observer, CancellationToken cancellationToken)
    {
        var statusCode = response.statusCode();
        if (statusCode >= 300)
        {
            observer.onError(new AIClientException("AI HTTP response status code is " + statusCode, null)); //$NON-NLS-1$
        }

        log.trace("AI response " + cancellationToken, response.toString()); //$NON-NLS-1$
        return response;
    }
}