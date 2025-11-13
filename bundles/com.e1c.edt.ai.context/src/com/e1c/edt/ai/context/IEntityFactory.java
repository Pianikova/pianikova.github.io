/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.List;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.form.model.Form;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.context.DTO.FormEntity;
import com.e1c.edt.ai.context.DTO.MetaEntity;
import com.e1c.edt.ai.context.DTO.MethodEntity;
import com.e1c.edt.ai.context.DTO.ObjectEntity;

interface IEntityFactory
{
    Optional<FormEntity> createFormEntity(Form form, ICancellationToken cancellationToken);

    Optional<ObjectEntity> crateObjectEntity(Variable variable, ICompositeNode node, boolean detailed,
        ICancellationToken cancellationToken);

    Optional<ObjectEntity> crateObjectEntity(FeatureAccess featureAccess, ICompositeNode node, boolean detailed,
        ICancellationToken cancellationToken);

    Optional<MethodEntity> createMethodEntity(Invocation invocation, ICompositeNode node, boolean detailed,
        ICancellationToken cancellationToken);

    Optional<MethodEntity> createMethodEntity(Method method, ICompositeNode node, boolean detailed,
        ICancellationToken cancellationToken);

    MetaEntity createMetaEntity(IBmObject bmObject, ICancellationToken cancellationToken);

    Optional<List<String>> getEnvironments(EObject obj, ICancellationToken cancellationToken);

    Optional<List<String>> getAreas(EObject obj, ICancellationToken cancellationToken);
}