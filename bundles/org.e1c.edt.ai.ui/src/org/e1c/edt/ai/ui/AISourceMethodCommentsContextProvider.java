/**
/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CodeCompletionType;
import org.e1c.edt.ai.IAIContextFactory;
import org.e1c.edt.ai.ICancellationToken;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslMultiLineCommentDocumentationProvider;
import com._1c.g5.v8.dt.bsl.model.Method;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class AISourceMethodCommentsContextProvider
    implements IAIContextProvider<AISourceContext>
{
    private final IAIContextFactory contextFactory;
    private final BslMultiLineCommentDocumentationProvider commentProvider;

    @Inject
    public AISourceMethodCommentsContextProvider(IAIContextFactory contextFactory,
        BslMultiLineCommentDocumentationProvider commentProvider)
    {
        Preconditions.checkNotNull(contextFactory);
        Preconditions.checkNotNull(commentProvider);
        this.contextFactory = contextFactory;
        this.commentProvider = commentProvider;
    }

    @Override
    public Optional<AIContext> create(AISourceContext ctx, ICancellationToken cancellationToken)
    {
        var parseResult = ctx.getParseResult();
        var offset = ctx.getOffset();
        var rootNoode = parseResult.getRootNode();
        var cursorNode = NodeModelUtils.findLeafNodeAtOffset(rootNoode, offset);
        var sibling = cursorNode.getNextSibling();
        var siblingSemantic = NodeModelUtils.findActualSemanticObjectFor(sibling);
        if (!(siblingSemantic instanceof Method))
        {
            return Optional.empty();
        }

        var comment = commentProvider.getDocumentation(siblingSemantic);
        if (!comment.isEmpty() && !comment.trim().endsWith("//")) //$NON-NLS-1$
        {
            return Optional.empty();
        }

        var method = new StringBuilder();
        var text = ctx.getViewer().getTextWidget().getText();
        if (text.endsWith("//") || text.endsWith("// ")) //$NON-NLS-1$//$NON-NLS-2$
        {
            method.append("//\n"); //$NON-NLS-1$
        }

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

        return contextFactory.create(method.toString(), offset, CodeCompletionType.Comments);
    }
}
