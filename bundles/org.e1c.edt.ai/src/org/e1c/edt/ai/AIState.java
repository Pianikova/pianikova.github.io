/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import com.google.common.base.Preconditions;

public class AIState
{
    private final ServiceState serviceState;
    private final ActionState actionState;

    public AIState(ServiceState serviceState, ActionState actionState)
    {
        Preconditions.checkNotNull(serviceState);
        Preconditions.checkNotNull(actionState);
        this.serviceState = serviceState;
        this.actionState = actionState;
    }

    public ServiceState getServiceState()
    {
        return serviceState;
    }

    public ActionState getActionState()
    {
        return actionState;
    }
}
