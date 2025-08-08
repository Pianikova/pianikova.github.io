/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.URL;
import java.net.http.HttpRequest;
import java.util.Optional;
import java.util.function.Function;

import com.e1c.edt.ai.client.AISettings;

public interface IRequestBuilder
{
    Optional<HttpRequest.Builder> create(Function<AISettings, URL> urlSelector, String relativePath);
}