/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.CancellationToken;
import org.eclipse.xtext.nodemodel.INode;

public interface ISyntaxWalker<TContext>
{
    void walk(INode targetNode, ISyntaxVisitor<TContext> visitor, SyntaxWalkerContext<TContext> ctx,
        CancellationToken cancellationToken);
}
