/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ContextInitializer
    implements IContextInitializer
{
    private final IContextSplitter contextSplitter;
    private final IStringNormalizer stringNormalizer;

    @Inject
    public ContextInitializer(IContextSplitter contextSplitter,
        IStringNormalizer stringNormalizer)
    {
        Preconditions.checkNotNull(contextSplitter);
        Preconditions.checkNotNull(stringNormalizer);
        this.contextSplitter = contextSplitter;
        this.stringNormalizer = stringNormalizer;
    }

    @Override
    public Optional<AIContext> initialize(AIContext ctx)
    {
        Preconditions.checkNotNull(ctx);
        var text = ctx.getText();
        if (text.isEmpty())
        {
            return Optional
                .of(new AIContext(ctx.getSource(), ctx.getSourceOffset(), ctx.getPath(), text,
                    ctx.getTextOffset()));
        }

        var source = ctx.getSource();
        var sourceOffset = ctx.getSourceOffset();
        if (sourceOffset > source.length())
        {
            sourceOffset = source.length();
        }

        var offset = ctx.getTextOffset();
        if (offset > text.length())
        {
            offset = text.length();
        }

        var parts = contextSplitter.split(text, offset);
        var prefix = stringNormalizer.normalize(parts.getPrefix().apply(text), true);
        var sufix = stringNormalizer.normalize(parts.getSufix().apply(text), true);
        return Optional
            .of(new AIContext(source, sourceOffset, ctx.getPath(), text, offset, prefix, sufix,
                sourceOffset - parts.getPrefix().getLength(), sourceOffset + parts.getSufix().getLength()));
    }
}