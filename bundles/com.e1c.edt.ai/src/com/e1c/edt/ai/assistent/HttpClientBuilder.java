/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;

class HttpClientBuilder
    implements IHttpClientBuilder
{
    private volatile HttpClient client;

    @Override
    public HttpClient get()
    {
        var result = client;
        if (result == null)
        {
            synchronized (this)
            {
                result = client;
                if (result == null)
                {
                    result = create().build();
                    client = result;
                }
            }
        }
        return result;
    }

    private HttpClient.Builder create()
    {
        // Use HTTP/1.1 on purpose: the shared client is hit by continuous code completion, which fires and cancels
        // requests constantly. HTTP/2 multiplexes everything over a single connection, so a cancelled/timed-out
        // request can wedge that connection and stall every other in-flight request. HTTP/1.1 gives each request its
        // own pooled connection, so a cancellation only closes its own connection and never affects the others.
        return HttpClient.newBuilder()
            .version(Version.HTTP_1_1)
            .followRedirects(Redirect.NORMAL)
            .proxy(ProxySelector.getDefault());
    }
}
