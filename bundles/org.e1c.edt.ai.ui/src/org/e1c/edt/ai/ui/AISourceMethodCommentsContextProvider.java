/**
/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IContextInitializer;
import org.e1c.edt.ai.context.CodePart;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AISourceMethodCommentsContextProvider
    implements IAIContextProvider<AISourceContext>
{
    private final IContextInitializer contextInitializer;

    @Inject
    public AISourceMethodCommentsContextProvider(IContextInitializer contextInitializer)
    {
        Preconditions.checkNotNull(contextInitializer);
        this.contextInitializer = contextInitializer;
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
                switch (part.getLocation())
                {
                case Comment:
                case OutsideFunction:
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

        return contextInitializer.initialize(
            new AIContext(ctx.getOffset(), target.getTextWidget().getText(), ctx.getOffset(), "", method.toString(), //$NON-NLS-1$
                offset));
    }
}
