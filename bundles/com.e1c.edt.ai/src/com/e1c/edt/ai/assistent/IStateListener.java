/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.ServiceState;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IStateListener
{
    void onServiceStateChange(ServiceState serviceState);

    void onActionStateChange(ActionState actionState);
}
