/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.form.model.Form;

public interface IEntityVisitor
{
    void visitForm(Form form);

    boolean visitInvocation(String nodeId, Invocation invocation, ICompositeNode node);

    boolean visitFeatureAccess(String nodeId, FeatureAccess featureAccess, ICompositeNode node);

    boolean visitVariable(String nodeId, Variable variable, ICompositeNode node);
}
