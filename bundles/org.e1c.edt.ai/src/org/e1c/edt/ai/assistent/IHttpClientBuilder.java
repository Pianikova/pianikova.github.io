/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.http.HttpClient;

public interface IHttpClientBuilder
{
    HttpClient.Builder create();
}
