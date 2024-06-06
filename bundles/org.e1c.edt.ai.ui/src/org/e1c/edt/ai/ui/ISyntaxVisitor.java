/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;

public interface ISyntaxVisitor<TContext>
{
    boolean visitNode(INode node, TContext ctx);

    boolean visitLeafNode(ILeafNode node, TContext ctx);
}
