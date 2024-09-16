/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

public interface IEndpointViewModel
{
    boolean isActive();

    void activate(int port);

    void deactivate();

    void restore();
}
