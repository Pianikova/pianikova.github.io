/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.net.http.HttpClient;

public interface IHttpClientBuilder
{
    /**
     * Returns a shared, lazily created {@link HttpClient} instance.
     * <p>
     * {@link HttpClient} is thread-safe and intended to be reused. Reuse this instance instead of building a new
     * client per request: each {@code create().build()} call spawns a dedicated selector thread and worker thread
     * pool that are kept alive until the client is garbage collected (the client is not closeable on JDK 17), which
     * leads to thread leaks under load.
     *
     * @return the shared client, never {@code null}.
     */
    HttpClient get();
}
