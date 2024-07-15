/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.e1c.edt.ai.ILog;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class HttpLog implements IHttpLog
{
    private final ILog log;

    @Inject
    public HttpLog(ILog log)
    {
        Preconditions.checkNotNull(log);
        this.log = log;
    }

    @Override
    public HttpRequest request(HttpRequest request, String ref, String body)
    {
        Preconditions.checkNotNull(request);
        var sb = new StringBuilder();
        sb.append(request);
        if (body != null)
        {
            sb.append(", "); //$NON-NLS-1$
            sb.append(body);
        }

        log.trace(createHeader("AI request", request.uri(), ref), sb.toString()); //$NON-NLS-1$
        return request;
    }

    @SuppressWarnings("nls")
    @Override
    public <T> HttpResponse<T> response(HttpResponse<T> response, String ref)
    {
        Preconditions.checkNotNull(response);
        var statusCode = response.statusCode();
        if (statusCode >= 300)
        {
            var sb = new StringBuilder();
            sb.append(createHeader("AI response", response.uri(), ref));
            sb.append(", ");
            sb.append(response.toString());
            sb.append(", ");
            sb.append(response.body());
            log.logError(sb.toString());
        }
        else
        {
            log.trace(createHeader("AI response", response.uri(), ref), response.toString() + ", " + response.body());
        }

        return response;
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
