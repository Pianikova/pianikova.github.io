/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.semantic;

interface IEndpointViewModel
{
    boolean isActive();

    void activate();

    void deactivate();

    void restore();

    int getPort();

    void setPort(int port);
}
