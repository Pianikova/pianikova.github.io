/**
 *
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.Observables;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.ToolFeedbackFinalTextRequest;
import com.e1c.edt.ai.assistent.model.ToolFeedbackResponse;
import com.e1c.edt.ai.assistent.model.ToolInvokeRequest;
import com.e1c.edt.ai.assistent.model.ToolInvokeResponse;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class Tools
    implements ITools
{
    private final IHttpLog log;
    private final ISettings settings;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clientBuilder;
    private final IJson json;
    private final ICompressor compressor;
    private final ISessionCall sessionCall;

    @Inject
    public Tools(IHttpLog log, ISettings settings, IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder,
        IJson json, ICompressor compressor, ISessionCall sessionCall)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(compressor);
        Preconditions.checkNotNull(sessionCall);
        this.log = log;
        this.settings = settings;
        this.requestBuilder = requestBuilder;
        this.clientBuilder = clientBuilder;
        this.json = json;
        this.compressor = compressor;
        this.sessionCall = sessionCall;
    }

    @Override
    public IObservable<ToolInvokeResponse> createInvokeSource(ProjectId projectId, ToolInvokeRequest toolInvokeRequest,
        ICancellationToken cancellationToken)
    {
        return Observables.create(observer -> {
            invoke(projectId, toolInvokeRequest, observer, cancellationToken);
            return Closeables.Empty;
        });
    }

    @Override
    public CompletableFuture<Optional<ToolFeedbackResponse>> feedbackAsync(ProjectId projectId,
        ToolFeedbackFinalTextRequest feedbackRequest, ICancellationToken cancellationToken)
    {
        var optionalRequestBuilder = requestBuilder.create(settings.getUrl() + "tools_api/v1/feedbacks/final_text"); //$NON-NLS-1$

        if (optionalRequestBuilder.isEmpty())
        {
            return CompletableFuture.completedFuture(Optional.empty());
        }

        var requestBuilder = optionalRequestBuilder.get();
        var requestBody = json.serialize(feedbackRequest);

        BodyPublisher bodyPublisher;
        try
        {
            bodyPublisher = BodyPublishers.ofString(requestBody);
        }
        catch (Exception error)
        {
            log.error(error, cancellationToken.toString());
            return CompletableFuture.failedFuture(error);
        }

        var client = clientBuilder.create().build();
        var currentRequestBuilder = requestBuilder.POST(bodyPublisher);
        var call = sessionCall.call(projectId, cancellationToken, session -> {
            var request =
                currentRequestBuilder.header("Session-Id", session.get().sessionId).POST(bodyPublisher).build(); //$NON-NLS-1$
            log.request(request, cancellationToken.toString(), requestBody);
            return client.sendAsync(request, BodyHandlers.ofString());
        });

        return call.orTimeout(settings.getTimeout().toNanos(), TimeUnit.NANOSECONDS)
            .thenApply(HttpResponse::body)
            .thenApply(this::createToolFeedbackResponse);
    }

    private void invoke(ProjectId projectId, ToolInvokeRequest conversationRequest,
        IObserver<ToolInvokeResponse> observer,
        ICancellationToken cancellationToken)
    {
        var optionalRequestBuilder =
            requestBuilder.create(settings.getUrl() + "tools_api/v1/invoke"); //$NON-NLS-1$
        if (optionalRequestBuilder.isEmpty())
        {
            observer.onCompleted();
            return;
        }

        var requestBuilder = optionalRequestBuilder.get();
        var requestBody = json.serialize(conversationRequest);
        BodyPublisher bodyPublisher;
        try
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
        catch (Exception error)
        {
            log.error(error, cancellationToken.toString());
            observer.onError(error);
            return;
        }

        var client = clientBuilder.create().build();
        var currentRequestBuilder = requestBuilder;
        var call = sessionCall.call(projectId, cancellationToken, session -> {
            var request =
                currentRequestBuilder.header("Session-Id", session.get().sessionId).POST(bodyPublisher).build(); //$NON-NLS-1$
            log.request(request, cancellationToken.toString(), requestBody);
            return client.sendAsync(request, BodyHandlers.ofLines());
        });

        call
            .orTimeout(settings.getTimeout().toNanos(), TimeUnit.NANOSECONDS)
            .thenApply(HttpResponse::body)
            .thenAccept(stream -> processStream(stream, observer, cancellationToken))
            .whenComplete((r, error) -> {
                if (error != null)
                {
                    observer.onError(error);
                }
            });
    }

    private void processStream(Stream<String> stream, IObserver<ToolInvokeResponse> observer,
        ICancellationToken cancellationToken)
    {
        stream.takeWhile(line -> process(line, observer, cancellationToken))
            .collect(Collectors.toList());
    }

    public boolean process(String line, IObserver<ToolInvokeResponse> observer,
        ICancellationToken cancellationToken)
    {
        if (cancellationToken.isCanceled())
        {
            observer.onCompleted();
            return false;
        }

        try
        {
            if (line == null || line.isBlank())
            {
                return true;
            }

            var sb = new StringBuilder(line.length() + 2);
            sb.append('{');
            sb.append(line);
            sb.append('}');
            var data = json.deserialize(sb.toString(), ToolInvokeResponsetreamData.class);
            if (data.isEmpty())
            {
                observer.onCompleted();
                return false;
            }

            var response = data.get().data;
            if (response == null)
            {
                observer.onCompleted();
                return false;
            }

            if (response.content == null)
            {
                return true;
            }

            observer.onNext(response);
            if (response.finished == false)
            {
                return true;
            }

            observer.onCompleted();
            return false;
        }
        catch (Exception error)
        {
            observer.onError(error);
        }
        finally
        {
            observer.onCompleted();
        }

        return false;
    }

    private Optional<ToolFeedbackResponse> createToolFeedbackResponse(String content)
    {
        return json.deserialize(content, ToolFeedbackResponse.class);
    }

    private static class ToolInvokeResponsetreamData
    {
        public ToolInvokeResponse data;
    }
}
