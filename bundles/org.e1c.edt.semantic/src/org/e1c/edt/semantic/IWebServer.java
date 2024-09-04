/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

public interface IWebServer
{
    AutoCloseable start(WebServerSettings settings);
}
