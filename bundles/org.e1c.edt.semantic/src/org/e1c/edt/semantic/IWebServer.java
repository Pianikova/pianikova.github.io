/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.semantic;

interface IWebServer
{
    AutoCloseable start(WebServerSettings settings);
}
