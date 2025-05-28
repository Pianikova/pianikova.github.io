/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.jface.text.source.SourceViewer;

interface IVerticalRulerManager
{
    AutoCloseable activate(SourceViewer viewer, Runnable onReset);

    AutoCloseable freeze(SourceViewer viewer);

    void reset(SourceViewer viewer);

    void redraw(SourceViewer viewer);
}
