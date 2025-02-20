/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui.handlers;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.Range;
import org.eclipse.jface.text.source.SourceViewer;

class TargetMethod
{
    public AIContext ctx;

    public SourceViewer sourceViewer;

    public String methodText;

    public Range commentRange;
}