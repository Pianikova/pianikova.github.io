/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

public interface IHttpLog
{
    HttpRequest request(HttpRequest request, String ref, String body);

    <T> HttpResponse<T> response(HttpResponse<T> response, String ref);

    void error(Throwable error, String ref);

    void error(String error, String ref);
}
