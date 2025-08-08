/**
 *
 */
package com.e1c.edt.ai.assistent;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpRequest.BodyPublisher;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.Closeables;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IObservable;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.IUISettings;
import com.e1c.edt.ai.Observables;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.Session;
import com.e1c.edt.ai.assistent.model.ToolInvokeRequest;
import com.e1c.edt.ai.assistent.model.ToolInvokeResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

public class Tools
    implements ITools
{
    private final IHttpLog log;
    private final IUISettings uiSettings;
    private final IRequestBuilder requestBuilder;
    private final IHttpClientBuilder clientBuilder;
    private final IJson json;
    private final ISessionService sessionService;
    private final ICompressor compressor;
    private final IStateService stateService;

    @Inject
    public Tools(IHttpLog log, IUISettings uiSettings,
        IRequestBuilder requestBuilder, IHttpClientBuilder clientBuilder, IJson json, ISessionService sessionService,
        ICompressor compressor, IStateService stateService)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(requestBuilder);
        Preconditions.checkNotNull(clientBuilder);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(sessionService);
        Preconditions.checkNotNull(compressor);
        Preconditions.checkNotNull(stateService);
        this.log = log;
        this.uiSettings = uiSettings;
        this.requestBuilder = requestBuilder;
        this.clientBuilder = clientBuilder;
        this.json = json;
        this.sessionService = sessionService;
        this.compressor = compressor;
        this.stateService = stateService;
    }

    @Override
    public IObservable<ToolInvokeResponse> createInvokeSource(ProjectId projectId, ToolInvokeRequest request,
        ICancellationToken cancellationToken)
    {
        return Observables.create(observer -> {
            sessionService.getSessionAsync(projectId).whenComplete((session, error) -> {
                if (error == null)
                {
                    if (session != null && session.isPresent())
                    {
                        invoke(session.get(), request, observer, cancellationToken);
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



    private void invoke(Session session, ToolInvokeRequest conversationRequest, IObserver<ToolInvokeResponse> observer,
        ICancellationToken cancellationToken)
    {
        var optionalRequest =
            requestBuilder.create(settings -> getUrl(settings.getLlmParameters().url), "tools_api/v1/invoke"); //$NON-NLS-1$
        if (optionalRequest.isEmpty())
        {
            observer.onCompleted();
            return;
        }

        var requestBuilder = optionalRequest.get().header("Session-Id", session.sessionId); //$NON-NLS-1$
        var requestBody = json.serialize(conversationRequest);
        BodyPublisher bodyPublisher;
        try
        {
            /*var optionalData = compressor.compress(requestBody);
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
            }*/

            bodyPublisher = BodyPublishers.ofString(requestBody);
        }
        catch (Exception error)
        {
            log.error(error, cancellationToken.toString());
            observer.onError(error);
            return;
        }

        var request = requestBuilder.POST(bodyPublisher).build();
        log.request(request, cancellationToken.toString(), requestBody);
        var stopwatch = Stopwatch.createStarted();
        var client = clientBuilder.create().build();
        var asyncRequest = client.sendAsync(request, BodyHandlers.ofLines());
        var attachToken = CancellationTokenSource.attach(cancellationToken, () -> {
            stateService.setState(CodeAssistant.class.getName(), ActionState.INACTIVE);
            asyncRequest.cancel(true);
        });
        asyncRequest
            .orTimeout(uiSettings.getTimeout().toNanos(), TimeUnit.NANOSECONDS)
            .thenApplyAsync(response -> log.response(response, cancellationToken.toString(), stopwatch, true))
            .thenApplyAsync(HttpResponse::body)
            .thenAcceptAsync(stream -> processStream(stream, observer, cancellationToken))
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

        observer.onCompleted();
        return false;
    }

    private URL getUrl(URL baseURL)
    {
        try
        {
            return baseURL.toURI().resolve("/").toURL(); //$NON-NLS-1$
        }
        catch (MalformedURLException | URISyntaxException e)
        {
            return baseURL;
        }
    }

    private static class ToolInvokeResponsetreamData
    {
        public ToolInvokeResponse data;
    }
}
