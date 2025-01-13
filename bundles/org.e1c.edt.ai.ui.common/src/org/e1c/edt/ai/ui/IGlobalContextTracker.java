/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.AIContext;

interface IGlobalContextTracker
{
    void track(AIContext aiCtx, boolean forcible);
}
