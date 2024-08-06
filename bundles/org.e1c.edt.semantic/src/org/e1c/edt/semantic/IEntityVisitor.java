/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Variable;

public interface IEntityVisitor
{
    boolean visitInvocation(String id, Invocation invocation, ICompositeNode node);

    boolean visitFeatureAccess(String id, FeatureAccess featureAccess, ICompositeNode node);

    boolean visitVariable(String id, Variable variable, ICompositeNode node);
}
