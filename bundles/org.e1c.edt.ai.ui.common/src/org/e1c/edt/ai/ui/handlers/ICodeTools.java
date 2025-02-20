/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import java.util.Optional;

import org.e1c.edt.ai.AIContext;

public interface ICodeTools
{
    boolean hasTarget();

    Optional<AIContext> createContextForTarget();

    Optional<TargetMethod> getTargetMethod();

    void selectMethodComment(TargetMethod commentingMethod);
}
