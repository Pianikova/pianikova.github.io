/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ContextSplitter
    implements IContextSplitter
{
    private final ISettings settings;

    @Inject
    public ContextSplitter(ISettings settings)
    {
        Preconditions.checkNotNull(settings);
        this.settings = settings;
    }

    @Override
    public ContextParts split(ProjectId projectId, String text, int offset, boolean limitSize)
    {
        Preconditions.checkNotNull(text);
        Preconditions.checkArgument(offset >= 0 && offset <= text.length());
        if (text.isEmpty())
        {
            return new ContextParts(Range.EMPTY, Range.EMPTY);
        }

        var maxPrefixLength = settings.getPrefixLength(projectId);
        if (maxPrefixLength < 0)
        {
            maxPrefixLength = 0;
        }

        var maxSuffixLength = settings.getSuffixLength(projectId);
        if (maxSuffixLength < 0)
        {
            maxSuffixLength = 0;
        }

        var length = text.length();
        if (offset == 0)
        {
            return new ContextParts(Range.EMPTY, new Range(0, Integer.min(length, maxSuffixLength)));
        }

        var start = limitSize ? Integer.max(offset - maxPrefixLength, 0) : 0;
        var finish = limitSize ? Integer.min(offset + maxSuffixLength - 1, length - 1) : length - 1;
        var prefix = new Range(start, offset - start);
        var suffix = new Range(offset, finish - offset + 1);
        return new ContextParts(prefix, suffix);
    }
}