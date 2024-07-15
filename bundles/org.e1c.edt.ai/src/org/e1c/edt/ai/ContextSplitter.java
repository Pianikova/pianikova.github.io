/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import com.google.common.base.Preconditions;

public class ContextSplitter implements IContextSplitter
{
    private Double midpointFactor;
    private String separator;

    public ContextSplitter()
    {
        this("\n", .6666); //$NON-NLS-1$
    }

    public ContextSplitter(String separator, Double midpointFactor)
    {
        Preconditions.checkNotNull(separator);
        Preconditions.checkArgument(separator.length() > 0);
        Preconditions.checkArgument(midpointFactor > 0 && midpointFactor < 1);
        this.separator = separator;
        this.midpointFactor = midpointFactor;
    }

    @Override
    public ContextParts split(String text, int offset, int maxLength)
    {
        Preconditions.checkNotNull(text);
        Preconditions.checkArgument(offset >= 0 && offset <= text.length());
        if (text.isEmpty())
        {
            return new ContextParts(Range.EMPTY, Range.EMPTY);
        }

        var length = text.length();
        if (length <= maxLength)
        {
            return new ContextParts(new Range(0, offset), new Range(offset, length - offset));
        }

        var maxPrefixLength = (int)(maxLength * midpointFactor + .5555);
        var maxSufixLength = maxLength - maxPrefixLength;
        var sufixFinish = offset + maxSufixLength - 1;
        if (sufixFinish >= length)
        {
            var dif = sufixFinish - length;
            maxPrefixLength += dif;
            sufixFinish -= dif;
        }

        var prefixStart = offset - maxPrefixLength;
        if (prefixStart < 0)
        {
            prefixStart = 0;
        }

        var separatorPosition = text.indexOf(separator, prefixStart);
        if (separatorPosition >= 0 && separatorPosition < offset)
        {
            var newVal = separatorPosition + 1;
            if (offset - newVal > 1)
            {
                prefixStart = newVal;
            }
        }

        separatorPosition = text.lastIndexOf(separator, sufixFinish);
        if (separatorPosition >= offset)
        {
            var newVal = separatorPosition + 1;
            if (newVal - offset > 1)
            {
                sufixFinish = separatorPosition - 1;
            }
        }

        var prefix = new Range(prefixStart, offset - prefixStart);

        var sufixLength = sufixFinish - offset + 1;
        if (offset + sufixLength > length)
        {
            sufixLength -= ((offset + sufixLength) - length);
        }

        var sufix = new Range(offset, sufixLength);
        return new ContextParts(prefix, sufix);
    }
}