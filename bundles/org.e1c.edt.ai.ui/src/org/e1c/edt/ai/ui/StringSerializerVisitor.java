/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.xtext.nodemodel.ILeafNode;

import com.google.common.base.Preconditions;

public class StringSerializerVisitor
    extends SyntaxVisitor<StringSerializerContext>
{
    @Override
    public boolean visitLeafNode(ILeafNode node, StringSerializerContext ctx)
    {
        Preconditions.checkNotNull(node);
        Preconditions.checkNotNull(ctx);
        return ctx.serialize(node) && super.visitLeafNode(node, ctx);
    }
}