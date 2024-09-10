/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.e1c.edt.ai.ICancellationToken;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.form.model.Form;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class EntitiesWalker
    implements IEntitiesWalker
{
    private final IV8Model v8Model;
    private final IIdFactory idFactory;

    @Inject
    public EntitiesWalker(IV8Model v8Model, IIdFactory idFactory)
    {
        Preconditions.checkNotNull(v8Model);
        Preconditions.checkNotNull(idFactory);
        this.v8Model = v8Model;
        this.idFactory = idFactory;
    }

    @Override
    public boolean walk(String path, int start, int finish, IEntityVisitor visitor,
        ICancellationToken cancellationToken)
    {
        var optionalModule = v8Model.getModule(path, cancellationToken);
        if (optionalModule.isEmpty())
        {
            return false;
        }

        var module = optionalModule.get();
        var owner = module.getOwner();
        if (owner instanceof Form)
        {
            visitor.visitForm((Form)owner);
        }

        var contentsIterator = module.eAllContents();
        while (contentsIterator.hasNext())
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            var obj = contentsIterator.next();
            if (obj instanceof Variable || obj instanceof Invocation || obj instanceof FeatureAccess
                || obj instanceof Method)
            {
                var node = v8Model.getNode(obj);
                var nodeStart = node.getTotalOffset();
                var nodeFinish = node.getTotalEndOffset();

                if (!((nodeStart >= start && nodeStart <= finish) || (nodeFinish >= start && nodeFinish <= finish))
                    && !(obj instanceof Method))
                {
                    continue;
                }

                var nodeId = idFactory.createNodeId(path, node);
                if (nodeId == null)
                {
                    continue;
                }

                if (obj instanceof Variable && visitor.visitVariable(nodeId, (Variable)obj, node))
                {
                    traceVisit(obj, true);
                    return true;
                }

                if (obj instanceof Invocation && visitor.visitInvocation(nodeId, (Invocation)obj, node))
                {
                    traceVisit(obj, true);
                    return true;
                }

                if (obj instanceof FeatureAccess && visitor.visitFeatureAccess(nodeId, (FeatureAccess)obj, node))
                {
                    traceVisit(obj, true);
                    return true;
                }

                if (obj instanceof Method && visitor.visitMethod(nodeId, (Method)obj, node))
                {
                    traceVisit(obj, true);
                    return true;
                }
            }

            traceVisit(obj, false);
        }

        return true;
    }

    private void traceVisit(EObject eObject, boolean visited)
    {
        /*System.out.println(visited + ", " + eObject.getClass().getName() + ": "
            + getNode(eObject).getText().replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"));*/
    }
}
