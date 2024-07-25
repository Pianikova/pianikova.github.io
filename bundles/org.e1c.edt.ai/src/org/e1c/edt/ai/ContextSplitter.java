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
        if (offset < maxPrefixLength)
        {
            maxSuffixLength += (maxPrefixLength - offset);
        }

        if ((length - offset) < maxSuffixLength)
        {
            maxPrefixLength += (maxSuffixLength - (length - offset));
        }

        var prefixLen = Integer.min(offset, maxPrefixLength);
        var prefix = new Range(offset - prefixLen, prefixLen);

        var suffixLen = Integer.min(length - offset, maxSuffixLength);
        var suffix = new Range(offset, suffixLen);

        return new ContextParts(prefix, suffix);
    }
}