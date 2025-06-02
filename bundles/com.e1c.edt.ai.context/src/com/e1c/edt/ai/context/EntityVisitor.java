/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
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
    public boolean visitModule(BmRoot root, Module module)
    {
        return false;
    }

    @Override
    public boolean visitNode(BmRoot root, EObject eObject, ICompositeNode node)
    {
        return false;
    }

    @Override
    public boolean visitOwnerAttribute(BmRoot root, IBmObject owner, BasicFeature attribute)
    {
        return false;
    }

    @Override
    public boolean visitBmObject(BmRoot root, IBmObject owner)
    {
        return false;
    }

    @Override
    public boolean visitOwnerTabularSection(BmRoot root, IBmObject owner,
        DbObjectTabularSection tabularSection)
    {
        return false;
    }

    @Override
    public boolean visitOwnerResource(BmRoot root, IBmObject owner, RegisterResource resource)
    {
        return false;
    }

    @Override
    public boolean visitOwnerDimension(BmRoot root, IBmObject owner, RegisterDimension dimension)
    {
        return false;
    }

    @Override
    public boolean visitOwnerRegisterRecord(BmRoot root, IBmObject owner, BasicRegister registerRecord)
    {
        return false;
    }

    @Override
    public boolean visitForm(BmRoot root, Form form)
    {
        return false;
    }

    @Override
    public boolean visitInvocation(BmRoot root, String nodeId, Invocation invocation, ICompositeNode node)
    {
        return false;
    }

    @Override
    public boolean visitFeatureAccess(BmRoot root, String nodeId, FeatureAccess featureAccess,
        ICompositeNode node)
    {
        return false;
    }

    @Override
    public boolean visitVariable(BmRoot root, String nodeId, Variable variable, ICompositeNode node)
    {
        return false;
    }

    @Override
    public boolean visitMethod(BmRoot root, String nodeId, Method method, ICompositeNode node)
    {
        return false;
    }
}
