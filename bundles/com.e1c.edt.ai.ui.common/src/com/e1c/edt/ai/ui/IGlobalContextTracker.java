/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.AIContext;

public interface IGlobalContextTracker
{
    void track(IProject project);

    void track(AIContext aiCtx);
}
