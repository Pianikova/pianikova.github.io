/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.xtext.nodemodel.ILeafNode;

import com.google.common.base.Preconditions;

public class SyntaxWalkerContext<TContext>
{
    private final TContext ctx;
    private final ILeafNode cursorNode;
    private final AISourceContext sourceCtx;

    public SyntaxWalkerContext(TContext ctx, AISourceContext sourceCtx, ILeafNode cursorNode)
    {
        Preconditions.checkNotNull(ctx);
        Preconditions.checkNotNull(sourceCtx);
        this.ctx = ctx;
        this.sourceCtx = sourceCtx;
        this.cursorNode = cursorNode;
    }

    public TContext getCtx()
    {
        return ctx;
    }

    public AISourceContext getSourceCtx()
    {
        return sourceCtx;
    }

    public ILeafNode getCursorNode()
    {
        return cursorNode;
    }
}
