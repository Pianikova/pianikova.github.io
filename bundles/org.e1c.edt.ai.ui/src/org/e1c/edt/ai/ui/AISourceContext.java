/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.ui.editor.XtextSourceViewer;

import com.google.common.base.Preconditions;

public class AISourceContext
{
    private final XtextSourceViewer viewer;
    private final IParseResult parseResult;
    private final int offset;
    private final int maxLength;
    public boolean SkipMinorMethodStatements;
    public boolean SkipMethodTail;
    public boolean SkipOutOfStackStatements;
    public boolean SkipMinorMethods;
    public boolean Forcable;

    public AISourceContext(XtextSourceViewer viewer, IParseResult parseResult, int offset, int maxLength)
    {
        Preconditions.checkNotNull(viewer);
        Preconditions.checkNotNull(parseResult);
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(maxLength > 0);
        Preconditions.checkNotNull(parseResult);
        this.viewer = viewer;
        this.parseResult = parseResult;
        this.offset = offset;
        this.maxLength = maxLength;
    }

    public XtextSourceViewer getViewer()
    {
        return viewer;
    }

    public IParseResult getParseResult()
    {
        return parseResult;
    }

    public int getOffset()
    {
        return offset;
    }

    public int getMaxLength()
    {
        return maxLength;
    }

    @SuppressWarnings("nls")
    @Override
    public String toString()
    {
        return "AISourceContext [offset=" + offset + ", maxLength=" + maxLength + ", SkipMinorMethodStatements="
            + SkipMinorMethodStatements + ", SkipMethodTail=" + SkipMethodTail + ", SkipOutOfStackStatements="
            + SkipOutOfStackStatements + ", SkipMinorMethods=" + SkipMinorMethods + ", Forcable=" + Forcable + "]";
    }
}
