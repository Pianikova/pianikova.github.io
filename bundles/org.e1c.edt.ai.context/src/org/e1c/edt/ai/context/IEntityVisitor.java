/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.metadata.mdclass.BasicFeature;
import com._1c.g5.v8.dt.metadata.mdclass.BasicRegister;
import com._1c.g5.v8.dt.metadata.mdclass.DbObjectTabularSection;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterDimension;
import com._1c.g5.v8.dt.metadata.mdclass.RegisterResource;

interface IEntityVisitor
{
    void visitModule(ModuleInfo moduleInfo);

    void visitNode(EObject eObject, ICompositeNode node);

    void visitOwnerAttribute(IBmObject owner, BasicFeature attribute);

    void visitOwnerTabularSection(IBmObject owner, DbObjectTabularSection tabularSection);

    void visitOwnerResource(IBmObject owner, RegisterResource resource);

    void visitOwnerDimension(IBmObject owner, RegisterDimension dimension);

    void visitOwnerRegisterRecord(IBmObject owner, BasicRegister registerRecord);

    void visitForm(Form form);

    boolean visitInvocation(String nodeId, Invocation invocation, ICompositeNode node);

    boolean visitFeatureAccess(String nodeId, FeatureAccess featureAccess, ICompositeNode node);

    boolean visitVariable(String nodeId, Variable variable, ICompositeNode node);

    boolean visitMethod(String nodeId, Method method, ICompositeNode node);
}
