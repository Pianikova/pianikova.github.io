/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStateService;
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

    @Inject
    public HttpLog(ILog log, IStateService stateService, IJson json)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(stateService);
        Preconditions.checkNotNull(json);
        this.json = json;
        this.log = log;
        this.stateService = stateService;
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
        var statusCode = response.statusCode();
        if (statusCode >= 200 && statusCode < 300)
        {
            if (handleError)
            {
                stateService.setState(ServiceState.ONLINE);
            }

            if (detailed)
            {
                if (stopwatch.elapsed().toMillis() < 1000)
                {
                    log.trace(TracingSources.API_CALLS, createHeader("AI response", response.uri(), ref),
                        () -> createTrace(response, stopwatch, statusCode));
                }
                else
                {
                    log.warning(createHeader("AI response", response.uri(), ref),
                        () -> createTrace(response, stopwatch, statusCode));
                }
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
                    var errorResponseOpt = json.deserialize(response.body().toString(), SessionErrorResponse.class);
                    if (errorResponseOpt.isPresent()) {
                        var errorResponse = errorResponseOpt.get();
                        if (errorResponse.errorType != null && errorResponse.errorType.equals("token_not_found")) {
                            stateService.setState(ServiceState.TOKEN_FAILED);
                        }
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
        sb.append(response.body());
        return sb.toString();
    }

    @Override
    public void error(Throwable error, String ref)
    {
        log.logError(error);
    }

    @Override
    public void error(String error, String ref)
    {
        log.logError(error);
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
