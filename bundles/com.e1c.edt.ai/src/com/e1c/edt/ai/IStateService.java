/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.IAIStateListener;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IStateService
{
    void setState(String className, ServiceState serviceState);

    void setState(String className, ActionState actionState);

    void refresh();

    void addListener(IAIStateListener serverAccessListener);

    void removeListener(IAIStateListener listener);

    String getLastClassOwner();
}
