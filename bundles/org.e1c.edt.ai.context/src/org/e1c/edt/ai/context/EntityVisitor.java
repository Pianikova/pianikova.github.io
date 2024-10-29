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

public class EntityVisitor
    implements IEntityVisitor
{
    @Override
    public void visitNode(EObject eObject, ICompositeNode node)
    {
        //
    }

    @Override
    public void visitOwnerAttribute(IBmObject owner, BasicFeature attribute)
    {
        //
    }

    @Override
    public void visitOwnerTabularSection(IBmObject owner, DbObjectTabularSection tabularSection)
    {
        //
    }

    @Override
    public void visitOwnerResource(IBmObject owner, RegisterResource resource)
    {
        //
    }

    @Override
    public void visitOwnerDimension(IBmObject owner, RegisterDimension dimension)
    {
        //
    }

    @Override
    public void visitOwnerRegisterRecord(IBmObject owner, BasicRegister registerRecord)
    {
        //
    }

    @Override
    public void visitForm(Form form)
    {
        //
    }

    @Override
    public boolean visitInvocation(String nodeId, Invocation invocation, ICompositeNode node)
    {
        return false;
    }

    @Override
    public boolean visitFeatureAccess(String nodeId, FeatureAccess featureAccess, ICompositeNode node)
    {
        return false;
    }

    @Override
    public boolean visitVariable(String nodeId, Variable variable, ICompositeNode node)
    {
        return false;
    }

    @Override
    public boolean visitMethod(String nodeId, Method method, ICompositeNode node)
    {
        return false;
    }
}
