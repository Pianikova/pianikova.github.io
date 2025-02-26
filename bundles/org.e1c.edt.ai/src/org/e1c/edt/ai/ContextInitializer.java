/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ContextInitializer
    implements IContextInitializer
{
    private final IContextSplitter contextSplitter;

    @Inject
    public ContextInitializer(IContextSplitter contextSplitter)
    {
        Preconditions.checkNotNull(contextSplitter);
        this.contextSplitter = contextSplitter;
    }

    @Override
    public Optional<AIContext> initialize(AIContext ctx)
    {
        Preconditions.checkNotNull(ctx);
        var text = ctx.getText();
        if (text.isEmpty())
        {
            return Optional
                .of(new AIContext(ctx.getProjectId(), AIContextKind.ActiveEditor, ctx.getСaretOffset(), ctx.getSource(),
                    ctx.getSourceOffset(),
                    ctx.getPath(), text,
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
        var prefix = parts.getPrefix().apply(text);
        var sufix = parts.getSufix().apply(text);
        return Optional
            .of(new AIContext(ctx.getProjectId(), AIContextKind.ActiveEditor, ctx.getСaretOffset(), source, sourceOffset,
                ctx.getPath(), text,
                offset, prefix, sufix,
                sourceOffset - parts.getPrefix().getLength(), sourceOffset + parts.getSufix().getLength()));
    }
}