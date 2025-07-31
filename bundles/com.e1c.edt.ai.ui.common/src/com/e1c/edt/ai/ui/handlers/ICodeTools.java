/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import java.util.Optional;

import org.eclipse.jface.text.source.SourceViewer;

import com.e1c.edt.ai.AIContext;

public interface ICodeTools
{
    boolean hasTarget(CodeAction action);

    Optional<AIContext> createContextForTarget(SourceViewer sourceViewer, CodeAction action);

    Optional<TargetMethod> getTargetMethod();

    void selectMethodComment(TargetMethod commentingMethod);
}
