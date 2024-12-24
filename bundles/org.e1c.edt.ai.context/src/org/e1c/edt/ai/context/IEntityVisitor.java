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
    boolean visitModule(ModuleInfo moduleInfo);

    boolean visitNode(ModuleInfo moduleInfo, EObject eObject, ICompositeNode node);

    boolean visitOwner(ModuleInfo moduleInfo, IBmObject owner);

    boolean visitOwnerAttribute(ModuleInfo moduleInfo, IBmObject owner, BasicFeature attribute);

    boolean visitOwnerTabularSection(ModuleInfo moduleInfo, IBmObject owner, DbObjectTabularSection tabularSection);

    boolean visitOwnerResource(ModuleInfo moduleInfo, IBmObject owner, RegisterResource resource);

    boolean visitOwnerDimension(ModuleInfo moduleInfo, IBmObject owner, RegisterDimension dimension);

    boolean visitOwnerRegisterRecord(ModuleInfo moduleInfo, IBmObject owner, BasicRegister registerRecord);

    boolean visitForm(ModuleInfo moduleInfo, Form form);

    boolean visitInvocation(ModuleInfo moduleInfo, String nodeId, Invocation invocation, ICompositeNode node);

    boolean visitFeatureAccess(ModuleInfo moduleInfo, String nodeId, FeatureAccess featureAccess, ICompositeNode node);

    boolean visitVariable(ModuleInfo moduleInfo, String nodeId, Variable variable, ICompositeNode node);

    boolean visitMethod(ModuleInfo moduleInfo, String nodeId, Method method, ICompositeNode node);
}
