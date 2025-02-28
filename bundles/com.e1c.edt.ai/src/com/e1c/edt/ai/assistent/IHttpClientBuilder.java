/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpClient;

public interface IHttpClientBuilder
{
    HttpClient.Builder create();
}
