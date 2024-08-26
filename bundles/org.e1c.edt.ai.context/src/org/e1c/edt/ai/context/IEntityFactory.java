/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.Optional;

import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Variable;

public interface IEntityFactory
{
    Optional<ObjectEntity> crateObjectEntity(Variable variable, ICompositeNode node);

    Optional<ObjectEntity> crateObjectEntity(FeatureAccess featureAccess, ICompositeNode node);

    Optional<MethodEntity> createMethodEntity(Invocation invocation);
}