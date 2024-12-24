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

class EntityVisitor
    implements IEntityVisitor
{
    @Override
    public boolean visitModule(ModuleInfo moduleInfo)
    {
        return false;
    }

    @Override
    public boolean visitNode(ModuleInfo moduleInfo, EObject eObject, ICompositeNode node)
    {
        return false;
    }

    @Override
    public boolean visitOwnerAttribute(ModuleInfo moduleInfo, IBmObject owner, BasicFeature attribute)
    {
        return false;
    }

    @Override
    public boolean visitOwner(ModuleInfo moduleInfo, IBmObject owner)
    {
        return false;
    }

    @Override
    public boolean visitOwnerTabularSection(ModuleInfo moduleInfo, IBmObject owner,
        DbObjectTabularSection tabularSection)
    {
        return false;
    }

    @Override
    public boolean visitOwnerResource(ModuleInfo moduleInfo, IBmObject owner, RegisterResource resource)
    {
        return false;
    }

    @Override
    public boolean visitOwnerDimension(ModuleInfo moduleInfo, IBmObject owner, RegisterDimension dimension)
    {
        return false;
    }

    @Override
    public boolean visitOwnerRegisterRecord(ModuleInfo moduleInfo, IBmObject owner, BasicRegister registerRecord)
    {
        return false;
    }

    @Override
    public boolean visitForm(ModuleInfo moduleInfo, Form form)
    {
        return false;
    }

    @Override
    public boolean visitInvocation(ModuleInfo moduleInfo, String nodeId, Invocation invocation, ICompositeNode node)
    {
        return false;
    }

    @Override
    public boolean visitFeatureAccess(ModuleInfo moduleInfo, String nodeId, FeatureAccess featureAccess,
        ICompositeNode node)
    {
        return false;
    }

    @Override
    public boolean visitVariable(ModuleInfo moduleInfo, String nodeId, Variable variable, ICompositeNode node)
    {
        return false;
    }

    @Override
    public boolean visitMethod(ModuleInfo moduleInfo, String nodeId, Method method, ICompositeNode node)
    {
        return false;
    }
}
