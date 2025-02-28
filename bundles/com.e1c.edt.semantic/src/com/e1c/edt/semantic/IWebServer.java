/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.semantic;

interface IWebServer
{
    AutoCloseable start(WebServerSettings settings);
}
