/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.handlers;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.Range;
import org.eclipse.jface.text.source.SourceViewer;

class TargetMethod
{
    public AIContext ctx;

    public SourceViewer sourceViewer;

    public String methodText;

    public Range commentRange;
}