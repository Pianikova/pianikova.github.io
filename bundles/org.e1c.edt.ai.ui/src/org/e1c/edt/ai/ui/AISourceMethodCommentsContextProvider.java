/**
/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.IAIContextFactory;
import org.e1c.edt.ai.ICancellationToken;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslMultiLineCommentDocumentationProvider;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AISourceMethodCommentsContextProvider
    implements IAIContextProvider<AISourceContext>
{
    private final IAIContextFactory contextFactory;

    @Inject
    public AISourceMethodCommentsContextProvider(IAIContextFactory contextFactory,
        BslMultiLineCommentDocumentationProvider commentProvider)
    {
        Preconditions.checkNotNull(contextFactory);
        Preconditions.checkNotNull(commentProvider);
        this.contextFactory = contextFactory;
    }

    @Override
    public Optional<AIContext> create(AITarget target, AISourceContext ctx, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(ctx);
        Preconditions.checkNotNull(cancellationToken);
        for (var part : ctx.getParts())
        {
            if (part.getRange().contains(ctx.getOffset()))
            {
                switch (part.getType())
                {
                case Comment:
                case MethodPrefix:
                    return create(target, ctx, part);

                default:
                    break;
                }

                break;
            }
        }

        return Optional.empty();
    }

    private Optional<AIContext> create(AITarget target, AISourceContext ctx, CodePart part)
    {
        var rootNode = ctx.getParseResult().getRootNode();
        var offset = ctx.getOffset();
        var cursorNode = NodeModelUtils.findLeafNodeAtOffset(rootNode, offset);
        var sibling = cursorNode.getNextSibling();
        var method = new StringBuilder();
        while (sibling != null)
        {
            method.append(sibling.getText());
            sibling = sibling.getNextSibling();
        }

        if (method.length() > 0 && method.charAt(0) == '&')
        {
            var parent = cursorNode.getParent();
            sibling = parent.getNextSibling();
            while (sibling != null)
            {
                method.append(sibling.getText());
                sibling = sibling.getNextSibling();
            }
        }

        return contextFactory.create(target.getTextWidget().getText(), ctx.getOffset(), method.toString(), offset,
            target.getComplitionType());
    }
}
