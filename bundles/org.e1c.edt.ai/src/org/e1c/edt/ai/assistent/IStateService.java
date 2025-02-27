/*
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent;

import org.e1c.edt.ai.ActionState;
import org.e1c.edt.ai.ServiceState;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IStateService
{
    void setState(String className, ServiceState serviceState);

    void setState(String className, ActionState actionState);

    void startMonitoring(int checkPeriodMs, int checkPeriodAfterErrorMs);

    void addListener(IAIStateListener serverAccessListener);

    void removeListener(IAIStateListener listener);
}
