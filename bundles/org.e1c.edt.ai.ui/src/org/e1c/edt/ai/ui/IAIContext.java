/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.jface.text.IDocument;

public interface IAIContext
{
    Optional<AIContext> create();

    Optional<AIContext> create(IDocument document, int cursorOffset);

    void apply(IDocument document, AIContext aiContext);
}
