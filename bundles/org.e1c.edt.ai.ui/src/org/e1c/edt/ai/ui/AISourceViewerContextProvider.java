/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CodeCompletionType;
import org.e1c.edt.ai.IAIContextFactory;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.ui.AIUIModule.SourceCodeSizeReducer;
import org.e1c.edt.ai.ui.AIUIModule.SourceMethodComments;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AISourceViewerContextProvider
    implements IAIContextProvider<AISourceContext>
{
    private final IAIContextFactory contextFactory;
    private final IAIContextProvider<AISourceContext> commentsContextProvider;
    private final IAIContextProvider<AISourceContext> sourceContextProvider;

    @Inject
    public AISourceViewerContextProvider(IAIContextFactory contextFactory,
        @SourceMethodComments IAIContextProvider<AISourceContext> commentsContextProvider,
        @SourceCodeSizeReducer IAIContextProvider<AISourceContext> sourceContextProvider)
    {
        Preconditions.checkNotNull(contextFactory);
        Preconditions.checkNotNull(commentsContextProvider);
        Preconditions.checkNotNull(sourceContextProvider);
        this.contextFactory = contextFactory;
        this.commentsContextProvider = commentsContextProvider;
        this.sourceContextProvider = sourceContextProvider;
    }

    @Override
    public Optional<AIContext> create(AISourceContext ctx, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(ctx);
        Preconditions.checkNotNull(cancellationToken);
        return commentsContextProvider.create(ctx, cancellationToken).or(() -> {
            ctx.SkipMinorMethodStatements = true;
            return sourceContextProvider.create(ctx, cancellationToken);
        }).or(() -> {
            var text = ctx.getViewer().getTextWidget().getText();
            if (text.length() <= ctx.getMaxLength())
            {
                return contextFactory.create(text, ctx.getOffset(), CodeCompletionType.Lines);
            }

            return Optional.empty();
        }).or(() -> {
            ctx.SkipMethodTail = true;
            return sourceContextProvider.create(ctx, cancellationToken);
        }).or(() -> {
            ctx.SkipOutOfStackStatements = true;
            return sourceContextProvider.create(ctx, cancellationToken);
        }).or(() -> {
            ctx.SkipMinorMethods = true;
            ctx.Forcable = true;
            return sourceContextProvider.create(ctx, cancellationToken);
        });
    }
}