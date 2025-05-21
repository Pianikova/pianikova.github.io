/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import java.util.Optional;

import org.eclipse.jface.text.source.SourceViewer;

import com.e1c.edt.ai.AIContext;

public interface ICodeTools
{
    boolean hasTarget();

    Optional<AIContext> createContextForTarget(SourceViewer sourceViewer);

    Optional<TargetMethod> getTargetMethod();

    void selectMethodComment(TargetMethod commentingMethod);
}
