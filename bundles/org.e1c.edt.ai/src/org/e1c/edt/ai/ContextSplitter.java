/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ContextSplitter implements IContextSplitter
{
    private final IContextSettings contextSettings;

    @Inject
    public ContextSplitter(IContextSettings contextSettings)
    {
        Preconditions.checkNotNull(contextSettings);
        this.contextSettings = contextSettings;
    }

    @Override
    public ContextParts split(String text, int offset)
    {
        Preconditions.checkNotNull(text);
        Preconditions.checkArgument(offset >= 0 && offset <= text.length());
        if (text.isEmpty())
        {
            return new ContextParts(Range.EMPTY, Range.EMPTY);
        }

        var maxPrefixLength = contextSettings.getPrefixLength();
        if (maxPrefixLength < 0)
        {
            maxPrefixLength = 0;
        }

        var maxSuffixLength = contextSettings.getSuffixLength();
        if (maxSuffixLength < 0)
        {
            maxSuffixLength = 0;
        }

        var length = text.length();
        if (offset == 0)
        {
            return new ContextParts(Range.EMPTY, new Range(0, Integer.min(length, maxSuffixLength)));
        }

        var start = Integer.max(offset - maxPrefixLength, 0);
        var finish = Integer.min(offset + maxSuffixLength - 1, length - 1);
        var prefix = new Range(start, offset - start);
        var suffix = new Range(offset, finish - offset + 1);
        return new ContextParts(prefix, suffix);
    }
}