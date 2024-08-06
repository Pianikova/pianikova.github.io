/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import org.e1c.edt.ai.ILog;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.mcore.AbstractMethod;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class RelatedEntities implements IRelatedEntities
{
    private final ILog log;
    private final IV8Model v8Model;
    private final IEntitiesWalker entitiesWalker;

    @Inject
    public RelatedEntities(ILog log, IV8Model v8Model, IEntitiesWalker entitiesWalker)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(v8Model);
        Preconditions.checkNotNull(entitiesWalker);
        this.log = log;
        this.v8Model = v8Model;
        this.entitiesWalker = entitiesWalker;
    }

    @SuppressWarnings("nls")
    @Override
    public Optional<RelatedEntitiesResponse> getRelatedEntities(RelatedEntitiesRequest request)
    {
        Preconditions.checkNotNull(request);
        if (request.path == null || request.path.isBlank())
        {
            return Optional.empty();
        }

        var response = new RelatedEntitiesResponse();
        response.relatedObjects = new ArrayList<>();
        response.relatedFunctions = new ArrayList<>();
        var ids = new HashSet<String>();
        var result = entitiesWalker.walk(request.path, request.span, new IEntityVisitor()
        {
            @Override
            public boolean visitVariable(String id, Variable variable, ICompositeNode node)
            {
                if (!ids.add(id))
                {
                    return false;
                }

                response.relatedObjects.add(id);
                traceEntity("object", id, variable, node);
                return false;
            }

            @Override
            public boolean visitFeatureAccess(String id, FeatureAccess featureAccess, ICompositeNode node)
            {
                if (!ids.add(id))
                {
                    return false;
                }

                for (var featureEntry : v8Model.getFeatureEntries(featureAccess))
                {
                    var feature = featureEntry.getFeature();
                    if (feature instanceof AbstractMethod)
                    {
                        return false;
                    }

                    if (feature instanceof Method)
                    {
                        return false;
                    }
                }

                response.relatedObjects.add(id);
                traceEntity("object", id, featureAccess, node);
                return false;
            }

            @Override
            public boolean visitInvocation(String id, Invocation invocation, ICompositeNode node)
            {
                if (!ids.add(id))
                {
                    return false;
                }

                response.relatedFunctions.add(id);
                traceEntity("function", id, invocation, node);
                return false;
            }
        });

        if (!result)
        {
            return Optional.empty();
        }

        return Optional.of(response);
    }

    @SuppressWarnings("nls")
    private void traceEntity(String type, String id, EObject eObject, ICompositeNode node)
    {
        var sb = new StringBuilder();
        sb.append("Node type:");
        sb.append(eObject.getClass().getName());
        sb.append(System.lineSeparator());
        sb.append("Code:");
        sb.append(System.lineSeparator());
        sb.append(node.getText());
        log.trace(type + ": " + id, sb.toString());
    }
}
