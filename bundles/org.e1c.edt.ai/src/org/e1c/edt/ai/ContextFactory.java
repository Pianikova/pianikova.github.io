/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ContextFactory
    implements IContextFactory
{
    private final IContextSplitter contextSplitter;
    private final IContextSettings contextSettings;
    private final IStringNormalizer stringNormalizer;

    @Inject
    public ContextFactory(IContextSplitter contextSplitter, IContextSettings contextSettings,
        IStringNormalizer stringNormalizer)
    {
        Preconditions.checkNotNull(contextSplitter);
        Preconditions.checkNotNull(contextSettings);
        Preconditions.checkNotNull(stringNormalizer);
        this.contextSplitter = contextSplitter;
        this.contextSettings = contextSettings;
        this.stringNormalizer = stringNormalizer;
    }

    @Override
    public Optional<AIContext> create(String source, int sourceOffset, String text, int offset)
    {
        Preconditions.checkNotNull(source);
        Preconditions.checkNotNull(text);
        Preconditions.checkArgument(offset >= 0);
        if (text.isEmpty())
        {
            return Optional.of(new AIContext(0, "", "", "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        }

        if (offset > text.length())
        {
            offset = text.length();
        }

        var parts = contextSplitter.split(text, offset, contextSettings.getMaxLength());
        var prefix = stringNormalizer.normalize(parts.getPrefix().apply(text), true);
        var sufix = stringNormalizer.normalize(parts.getSufix().apply(text), true);
        return Optional.of(new AIContext(offset, text, prefix, sufix));
    }
}