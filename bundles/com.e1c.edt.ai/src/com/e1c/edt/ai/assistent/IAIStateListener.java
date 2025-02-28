/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import com.e1c.edt.ai.AIState;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IAIStateListener
{
    void onStateChange(AIState state);
}
