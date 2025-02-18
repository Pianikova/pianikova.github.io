/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import org.e1c.edt.ai.AIState;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IAIStateListener
{
    void onStateChange(AIState state);
}
