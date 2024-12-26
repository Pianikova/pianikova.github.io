/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;

class HttpClientBuilder
    implements IHttpClientBuilder
{
    @Override
    public HttpClient.Builder create()
    {
        return HttpClient.newBuilder()
            .version(Version.HTTP_2)
            .followRedirects(Redirect.NORMAL)
            .proxy(ProxySelector.getDefault());
    }
}
