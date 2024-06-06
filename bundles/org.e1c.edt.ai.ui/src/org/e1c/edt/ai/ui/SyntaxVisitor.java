/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;

import com.google.common.base.Preconditions;

public class SyntaxVisitor<TContext>
    implements ISyntaxVisitor<TContext>
{
    @Override
    public boolean visitNode(INode node, TContext ctx)
    {
        Preconditions.checkNotNull(node);
        Preconditions.checkNotNull(ctx);
        if (node instanceof ILeafNode)
        {
            return visitLeafNode((ILeafNode)node, ctx);
        }

        return true;
    }

    @Override
    public boolean visitLeafNode(ILeafNode node, TContext ctx)
    {
        Preconditions.checkNotNull(node);
        Preconditions.checkNotNull(ctx);
        return true;
    }
}
