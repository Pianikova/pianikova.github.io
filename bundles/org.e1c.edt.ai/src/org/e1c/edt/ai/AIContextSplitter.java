/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import com.google.common.base.Preconditions;

public class AIContextSplitter implements IAIContextSplitter
{
    private Double midpointFactor;
    private String separator;

    public AIContextSplitter()
    {
        this("\n", .6666); //$NON-NLS-1$
    }

    public AIContextSplitter(String separator, Double midpointFactor)
    {
        Preconditions.checkNotNull(separator);
        Preconditions.checkArgument(separator.length() > 0);
        Preconditions.checkArgument(midpointFactor > 0 && midpointFactor < 1);
        this.separator = separator;
        this.midpointFactor = midpointFactor;
    }

    @Override
    public AIContextParts split(String text, int offset, int maxLength)
    {
        Preconditions.checkNotNull(text);
        Preconditions.checkArgument(offset >= 0 && offset <= text.length());
        return splitOnPrefixAndSufixAndMiddle(text, offset, maxLength);
    }

    private AIContextParts splitOnPrefixAndSufixAndMiddle(String text, int offset, int maxLength)
    {
        var parts = splitOnPrefixAndSufix(text, offset, maxLength);

        var prefix = parts.getPrefix();
        if (!prefix.isEmpty())
        {
            var prefixFinish = prefix.getStart() + prefix.getLength();
            var separatorPosition = text.substring(prefix.getStart(), prefixFinish).lastIndexOf(separator);
            if (separatorPosition >= 0)
            {
                separatorPosition = prefix.getStart() + separatorPosition + separator.length();
                var middleLength = prefixFinish - separatorPosition;
                var middle = new Range(separatorPosition, middleLength);
                var prefixLength = prefix.getLength() - middleLength;
                if (prefixLength > 0)
                {
                    prefix = new Range(prefix.getStart(), prefixLength);
                }
                else
                {
                    prefix = Range.EMPTY;
                }

                return new AIContextParts(prefix, parts.getSufix(), middle);
            }
            else
            {
                return new AIContextParts(Range.EMPTY, parts.getSufix(), prefix);
            }
        }

        return parts;
    }

    private AIContextParts splitOnPrefixAndSufix(String text, int offset, int maxLength)
    {
        if (text.isEmpty())
        {
            return new AIContextParts(Range.EMPTY, Range.EMPTY, Range.EMPTY);
        }

        var length = text.length();
        if (text.isBlank())
        {
            return new AIContextParts(new Range(0, length), Range.EMPTY, Range.EMPTY);
        }

        if (length <= maxLength)
        {
            return new AIContextParts(new Range(0, offset), new Range(offset, length - offset), Range.EMPTY);
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
        return new AIContextParts(prefix, sufix, Range.EMPTY);
    }
}
