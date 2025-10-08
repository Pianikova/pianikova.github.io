/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.google.common.base.Stopwatch;

public interface IHttpLog
{
    HttpRequest request(HttpRequest request, String ref, String body);

    <T> HttpResponse<T> response(HttpResponse<T> response, String ref, Stopwatch stopwatch, boolean detailed,
        boolean handleError);

    void error(Throwable error, String ref);

    void error(String error, String ref);
}
