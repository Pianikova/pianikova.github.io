/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import org.e1c.edt.ai.ServerAccessType;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IServerAccessService
{
    void accessChanged(String className, ServerAccessType status);

    void startMonitoring(int checkPeriodMs, int checkPeriodAfterErrorMs);

    void addServerAccessListener(IServerAccessListener serverAccessListener);
}
