/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.context;

import java.util.List;
import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.context.DTO.FormEntity;
import org.e1c.edt.ai.context.DTO.MetaEntity;
import org.e1c.edt.ai.context.DTO.MethodEntity;
import org.e1c.edt.ai.context.DTO.ObjectEntity;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

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

interface IEntityFactory
{
    Optional<FormEntity> createFormEntity(Form form,
        ICancellationToken cancellationToken);

    Optional<ObjectEntity> crateObjectEntity(Variable variable, ICompositeNode node, boolean detailed,
        ICancellationToken cancellationToken);

    Optional<ObjectEntity> crateObjectEntity(FeatureAccess featureAccess, ICompositeNode node, boolean detailed,
        ICancellationToken cancellationToken);

    Optional<MethodEntity> createMethodEntity(Invocation invocation, ICompositeNode node, boolean detailed,
        ICancellationToken cancellationToken);

    Optional<MethodEntity> createMethodEntity(Method method, ICompositeNode node, boolean detailed,
        ICancellationToken cancellationToken);

    Optional<MetaEntity> createMetaEntity(List<BasicFeature> attributes, List<DbObjectTabularSection> tabularSections,
        List<RegisterResource> registerResources, List<RegisterDimension> registerDimensions,
        List<BasicRegister> registerRecords, ICancellationToken cancellationToken);

    Optional<List<String>> getEnvironments(EObject obj);

    Optional<List<String>> getAreas(EObject obj);
}