/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.http.HttpRequest;
import java.util.Optional;

public interface IRequestBuilder
{
    Optional<HttpRequest.Builder> create(String relativePath);
}