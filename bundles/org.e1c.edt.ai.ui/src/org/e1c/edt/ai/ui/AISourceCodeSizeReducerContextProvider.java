/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IClock;
import org.e1c.edt.ai.IContextInitializer;
import org.e1c.edt.ai.ILog;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AISourceCodeSizeReducerContextProvider
    implements IAIContextProvider<AISourceContext>
{
    private final ILog log;
    private final IContextInitializer contextInitializer;
    private final ISyntaxWalker<StringSerializerContext> basicPathSyntaxWalker;
    private final ISyntaxVisitor<StringSerializerContext> serializerVisitor;
    private IClock clock;

    @Inject
    public AISourceCodeSizeReducerContextProvider(ILog log, IContextInitializer contextInitializer,
        ISyntaxWalker<StringSerializerContext> basicPathSyntaxWalker,
        ISyntaxVisitor<StringSerializerContext> serializerVisitor, IClock clock)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(contextInitializer);
        Preconditions.checkNotNull(basicPathSyntaxWalker);
        Preconditions.checkNotNull(serializerVisitor);
        Preconditions.checkNotNull(clock);
        this.log = log;
        this.contextInitializer = contextInitializer;
        this.basicPathSyntaxWalker = basicPathSyntaxWalker;
        this.serializerVisitor = serializerVisitor;
        this.clock = clock;
    }

    @Override
    public Optional<AIContext> create(AITarget target, AISourceContext ctx, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(ctx);
        Preconditions.checkNotNull(cancellationToken);
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

        var cursorNode = findVisibleLeafNodeAtOffset(rootNoode, offset);
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
            "length: " + textLength + ", duration: " + duration.toMillis() + " ms, " + ctx); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
        if (!ctx.Forcable && textLength > maxLength)
        {
            return Optional.empty();
        }

        return contextInitializer.initialize(
            new AIContext(target.getTextWidget().getText(), offset, "", text, //$NON-NLS-1$
                serializerContext.getOffset()));
    }

    private ILeafNode findVisibleLeafNodeAtOffset(INode rootNode, int leafNodeOffset)
    {
        ILeafNode lastVisible = null;
        for (var leafNode : rootNode.getLeafNodes())
        {
            if (!leafNode.isHidden())
            {
                lastVisible = leafNode;
            }

            if (leafNodeOffset >= leafNode.getOffset()
                && leafNodeOffset < leafNode.getTotalEndOffset())
            {
                if (leafNode.isHidden())
                {
                    return lastVisible;
                }

                return leafNode;
            }
        }

        return null;
    }
}
