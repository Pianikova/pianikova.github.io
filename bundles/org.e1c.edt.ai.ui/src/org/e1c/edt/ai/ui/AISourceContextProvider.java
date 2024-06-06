/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CancellationToken;
import org.e1c.edt.ai.IAIContextFactory;
import org.e1c.edt.ai.IClock;
import org.e1c.edt.ai.ILog;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AISourceContextProvider
    implements IAIContextProvider<AISourceContext>
{
    private final ILog log;
    private final IAIContextFactory contextFactory;
    private final ISyntaxWalker<StringSerializerContext> basicPathSyntaxWalker;
    private final ISyntaxVisitor<StringSerializerContext> serializerVisitor;
    private IClock clock;

    @Inject
    public AISourceContextProvider(ILog log, IAIContextFactory contextFactory,
        ISyntaxWalker<StringSerializerContext> basicPathSyntaxWalker,
        ISyntaxVisitor<StringSerializerContext> serializerVisitor, IClock clock)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(contextFactory);
        Preconditions.checkNotNull(basicPathSyntaxWalker);
        Preconditions.checkNotNull(serializerVisitor);
        Preconditions.checkNotNull(clock);
        this.log = log;
        this.contextFactory = contextFactory;
        this.basicPathSyntaxWalker = basicPathSyntaxWalker;
        this.serializerVisitor = serializerVisitor;
        this.clock = clock;
    }

    @Override
    public Optional<AIContext> create(AISourceContext ctx, CancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(ctx);
        var startTime = clock.now();
        var parseResult = ctx.getParseResult();
        /*if (parseResult.hasSyntaxErrors())
        {
            return Optional.empty();
        }*/

        var offset = ctx.getOffset();
        var rootNoode = parseResult.getRootNode();
        if (rootNoode == null || cancellationToken.isCanceled())
        {
            return Optional.empty();
        }

        var cursorNode = NodeModelUtils.findLeafNodeAtOffset(rootNoode, offset);
        if (cursorNode == null || cancellationToken.isCanceled())
        {
            return Optional.empty();
        }

        var maxLength = ctx.getMaxLength();
        var serializerContext =
            new StringSerializerContext(cursorNode, offset, ctx.Forcable ? Integer.MAX_VALUE : maxLength);
        var syntaxWalkerContext = new SyntaxWalkerContext<>(serializerContext, ctx, cursorNode);
        basicPathSyntaxWalker.walk(rootNoode, serializerVisitor, syntaxWalkerContext, cancellationToken);
        if (cancellationToken.isCanceled())
        {
            return Optional.empty();
        }

        var text = serializerContext.getText();
        var textLength = text.length();
        var duration = Duration.between(startTime, clock.now());
        log.trace("AI context optimizer " + cancellationToken, //$NON-NLS-1$
            "length: " + textLength + ", duration: " + duration.toMillis() + " ms, " + ctx); //$NON-NLS-1$ //$NON-NLS-2$
        if (!ctx.Forcable && textLength > maxLength)
        {
            return Optional.empty();
        }

        return contextFactory.create(text, serializerContext.getOffset());
    }
}
