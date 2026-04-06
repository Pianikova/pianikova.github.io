/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Optional;
import java.util.stream.Stream;

import javax.net.ssl.SSLSession;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ITraceScenario;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.model.SessionErrorResponse;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.inject.Inject;

class HttpLog
    implements IHttpLog
{
    private final ILog log;
    private final IJson json;
    private final IStateService stateService;
    private final ITraceScenario traceScenario;

    @Inject
    public HttpLog(ILog log, IStateService stateService, IJson json, ITraceScenario traceScenario)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(stateService);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(traceScenario);
        this.json = json;
        this.log = log;
        this.stateService = stateService;
        this.traceScenario = traceScenario;
    }

    @Override
    public HttpRequest request(HttpRequest request, String ref, String body)
    {
        Preconditions.checkNotNull(request);

        log.trace(TracingSources.API_CALLS, createHeader("AI request", request.uri(), ref), //$NON-NLS-1$
            () -> {
                var sb = new StringBuilder();
                sb.append(request);
                var headers = request.headers();
                if (headers != null)
                {
                    var headersMap = headers.map();
                    if (headersMap.size() != 0)
                    {
                        sb.append(System.lineSeparator());
                        sb.append("headers:"); //$NON-NLS-1$
                        for (var header : headersMap.entrySet())
                        {
                            sb.append(System.lineSeparator());
                            sb.append('\t');
                            sb.append(header.getKey());
                            sb.append('=');
                            sb.append(String.join(", ", header.getValue()));//$NON-NLS-1$
                        }
                    }
                }

                if (body != null)
                {
                    sb.append(System.lineSeparator());
                    sb.append("size: "); //$NON-NLS-1$
                    sb.append(body.length());
                    sb.append(System.lineSeparator());
                    sb.append("body:"); //$NON-NLS-1$
                    var it = body.lines().iterator();
                    while (it.hasNext())
                    {
                        sb.append(System.lineSeparator());
                        sb.append(it.next());
                    }
                }

                return sb.toString();
            });
        return request;
    }

    @SuppressWarnings("nls")
    @Override
    public <T> HttpResponse<T> response(HttpResponse<T> response, String ref, Stopwatch stopwatch, boolean detailed,
        boolean handleError)
    {
        Preconditions.checkNotNull(response);
        switch (traceScenario.getActive())
        {
        case SESSION_EXPIRED:
            response = createSessionExpiredResponse(response);
            break;
        case SESSION_EXPIRED_STREAM:
            response = createSessionExpiredStreamResponse(response);
            break;
        case TOKEN_NOT_FOUND:
            response = createTokenNotFoundResponse(response);
            break;
        case SERVER_ERROR:
            response = createServerErrorResponse(response);
            break;
        case NONE:
        default:
            break;
        }

        var statusCode = response.statusCode();
        if (statusCode >= 200 && statusCode < 300)
        {
            stateService.setState(ServiceState.ONLINE);
            if (detailed)
            {
                final HttpResponse<T> currentResponse = response;
                log.trace(TracingSources.API_CALLS, createHeader("AI response", currentResponse.uri(), ref),
                    () -> createTrace(currentResponse, stopwatch, statusCode));
            }
        }
        else
        {
            log.logError(new AIClientException(createHeader("AI response", response.uri(), ref) + System.lineSeparator()
                + createTrace(response, stopwatch, statusCode), null));

            if (handleError)
            {
                switch (statusCode)
                {
                case 401:
                case 403:
                    var errorType =
                        extractBody(response.body()).flatMap(body -> json.deserialize(body, SessionErrorResponse.class))
                            .map(i -> i.errorType)
                            .orElse("");

                    switch (errorType)
                    {
                    case "token_not_found":
                        stateService.setState(ServiceState.TOKEN_ERROR);
                        break;

                    case "invalid_session":
                        stateService.setState(ServiceState.SESSION_EXPIRED);
                        break;

                    default:
                        stateService.setState(ServiceState.SESSION_EXPIRED);
                    }

                    break;

                case 500:
                case 502:
                case 503:
                    stateService.setState(ServiceState.SERVER_ERROR);
                    break;

                default:
                    stateService.setState(ServiceState.OFFLINE);
                    break;
                }
            }
        }

        return response;
    }

    @SuppressWarnings("nls")
    private <T> String createTrace(HttpResponse<T> response, Stopwatch stopwatch, int statusCode)
    {
        var sb = new StringBuilder();
        sb.append("status code: ");
        sb.append(statusCode);
        sb.append(System.lineSeparator());
        sb.append("duration: ");
        sb.append(stopwatch.elapsed());
        sb.append(System.lineSeparator());
        sb.append(response.toString());
        sb.append(System.lineSeparator());
        var body = response.body();
        if (body != null)
        {
            if (body instanceof Stream)
            {
                sb.append("body type: Stream (not logged to preserve for processing)"); //$NON-NLS-1$
            }
            else
            {
                var bodyStr = extractBody(body).orElse(body.toString());
                sb.append("size: ");
                sb.append(bodyStr.length());
                sb.append(System.lineSeparator());
                sb.append("body:");
                var it = bodyStr.lines().iterator();
                while (it.hasNext())
                {
                    sb.append(System.lineSeparator());
                    sb.append('\t');
                    sb.append(it.next());
                }
            }
        }
        return sb.toString();
    }

    private <T> HttpResponse<T> createReplacementResponse(HttpResponse<T> originalResponse, int statusCode, Object body)
    {
        return new HttpResponse<>()
        {
            @Override
            public int statusCode()
            {
                return statusCode;
            }

            @Override
            public HttpRequest request()
            {
                return originalResponse.request();
            }

            @Override
            public Optional<HttpResponse<T>> previousResponse()
            {
                return originalResponse.previousResponse();
            }

            @Override
            public java.net.http.HttpHeaders headers()
            {
                return originalResponse.headers();
            }

            @SuppressWarnings("unchecked")
            @Override
            public T body()
            {
                return (T)body;
            }

            @Override
            public URI uri()
            {
                return originalResponse.uri();
            }

            @Override
            public HttpClient.Version version()
            {
                return originalResponse.version();
            }

            @Override
            public Optional<SSLSession> sslSession()
            {
                return Optional.empty();
            }
        };
    }

    private <T> HttpResponse<T> createSessionExpiredResponse(HttpResponse<T> originalResponse)
    {
        return createErrorResponse(originalResponse, "Session expired", "invalid_session", 401); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private <T> HttpResponse<T> createSessionExpiredStreamResponse(HttpResponse<T> originalResponse)
    {
        return createErrorResponse(originalResponse, "Session expired", "invalid_session", 401); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private <T> HttpResponse<T> createTokenNotFoundResponse(HttpResponse<T> originalResponse)
    {
        return createErrorResponse(originalResponse, "Token not found", "token_not_found", 401); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private <T> HttpResponse<T> createServerErrorResponse(HttpResponse<T> originalResponse)
    {
        return createErrorResponse(originalResponse, "Internal server error", "internal_server_error", 500); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private <T> HttpResponse<T> createErrorResponse(HttpResponse<T> originalResponse, String errorText,
        String errorType, int statusCode)
    {
        var sessionErrorResponse = new SessionErrorResponse();
        sessionErrorResponse.error = errorText;
        sessionErrorResponse.errorType = errorType;
        var errorBody = json.serialize(sessionErrorResponse);

        Object body = errorBody;
        if (originalResponse.body() instanceof Stream)
        {
            body = java.util.stream.Stream.<String> of(errorBody);
        }

        return createReplacementResponse(originalResponse, statusCode, body);
    }

    @Override
    public void error(Throwable error, String ref)
    {
        log.trace(TracingSources.API_CALLS, "API error", () -> error.toString()); //$NON-NLS-1$
    }

    @Override
    public void error(String error, String ref)
    {
        log.trace(TracingSources.API_CALLS, "API error", () -> error); //$NON-NLS-1$
    }

    private Optional<String> extractBody(Object body)
    {
        try
        {
            if (body instanceof String)
            {
                return Optional.ofNullable((String)body);
            }
            else if (body instanceof Stream)
            {
                try
                {
                    @SuppressWarnings("unchecked")
                    var stream = (Stream<String>)body;
                    return Optional.of(stream.collect(java.util.stream.Collectors.joining(""))); //$NON-NLS-1$
                }
                catch (ClassCastException e)
                {
                    return Optional.empty();
                }
            }
        }
        catch (Exception error)
        {
            //
        }

        return Optional.empty();
    }

    private String createHeader(String name, URI uri, String ref)
    {
        var sb = new StringBuilder();
        sb.append(name);
        sb.append(' ');
        sb.append(uri.getPath());
        if (ref != null && !ref.isBlank())
        {
            sb.append(' ');
            sb.append(ref);
        }

        return sb.toString();
    }
}
