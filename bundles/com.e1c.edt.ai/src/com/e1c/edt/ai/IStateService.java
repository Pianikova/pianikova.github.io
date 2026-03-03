/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.IStateListener;

public interface IStateService
{
    AIState getState();

    void setState(ServiceState serviceState);

    AutoCloseable busy();

    void refresh();

    void addListener(IStateListener serverAccessListener);

    void removeListener(IStateListener listener);
}
