/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.xtext.parser.IParseResult;

import com.google.common.base.Preconditions;

public class AISourceContext
{
    private final IParseResult parseResult;
    private final int offset;
    private final int maxLength;
    public boolean SkipMinorMethodStatements;
    public boolean SkipMethodTail;
    public boolean SkipOutOfStackStatements;
    public boolean SkipMinorMethods;
    public boolean Forcable;

    public AISourceContext(IParseResult parseResult, int offset, int maxLength)
    {
        Preconditions.checkNotNull(parseResult);
        Preconditions.checkArgument(offset >= 0);
        Preconditions.checkArgument(maxLength > 0);
        Preconditions.checkNotNull(parseResult);
        this.parseResult = parseResult;
        this.offset = offset;
        this.maxLength = maxLength;
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
