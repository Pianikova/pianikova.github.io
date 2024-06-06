/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import com.google.common.base.Preconditions;

public class WalkerContext<TContext>
{
    private final ISyntaxVisitor<TContext> visitor;
    private final TContext ctx;

    public WalkerContext(ISyntaxVisitor<TContext> visitor, TContext ctx)
    {
        Preconditions.checkNotNull(visitor);
        Preconditions.checkNotNull(ctx);
        this.visitor = visitor;
        this.ctx = ctx;
    }

    public ISyntaxVisitor<TContext> getVisitor()
    {
        return visitor;
    }

    public TContext getCtx()
    {
        return ctx;
    }
}
