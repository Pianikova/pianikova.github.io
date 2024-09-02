/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.context.DTO.FormEntity;
import org.e1c.edt.ai.context.DTO.MethodEntity;
import org.e1c.edt.ai.context.DTO.ObjectEntity;
import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.form.model.Form;

public interface IEntityFactory
{
    Optional<FormEntity> createFormEntity(Form form, ICancellationToken cancellationToken);

    Optional<ObjectEntity> crateObjectEntity(Variable variable, ICompositeNode node,
        ICancellationToken cancellationToken);

    Optional<ObjectEntity> crateObjectEntity(FeatureAccess featureAccess, ICompositeNode node,
        ICancellationToken cancellationToken);

    Optional<MethodEntity> createMethodEntity(Invocation invocation, ICompositeNode node,
        ICancellationToken cancellationToken);
}