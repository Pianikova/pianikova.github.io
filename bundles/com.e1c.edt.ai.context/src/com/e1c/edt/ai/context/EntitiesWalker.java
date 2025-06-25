/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.IDocument;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com._1c.g5.v8.dt.form.model.Form;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.StatisticsType;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class EntitiesWalker
    implements IEntitiesWalker
{
    private final ILog log;
    private final IV8Model v8Model;
    private final IIdFactory idFactory;
    private final IBmPovider bmPovider;

    @Inject
    public EntitiesWalker(ILog log, IV8Model v8Model, IIdFactory idFactory,
        IBmPovider bmPovider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(v8Model);
        Preconditions.checkNotNull(idFactory);
        Preconditions.checkNotNull(bmPovider);
        this.log = log;
        this.v8Model = v8Model;
        this.idFactory = idFactory;
        this.bmPovider = bmPovider;
    }

    @Override
    public boolean walk(IDocument document, String path, int start, int finish, IModuleProvider resourceSetProvider,
        IEntityVisitor visitor,
        IStatistics statistics, ICancellationToken cancellationToken)
    {
        try
        {
            Optional<BmRoot> optionalRoot;
            try (var measurement = statistics.measureDuration(StatisticsType.LOAD_MODULE_DURATUION))
            {
                optionalRoot = bmPovider.getRoot(path, cancellationToken);
                if (optionalRoot.isEmpty())
                {
                    return false;
                }
            }

            var root = optionalRoot.get();
            EObject nextObject = null;
            while (true)
            {
                if (cancellationToken.isCanceled())
                {
                    break;
                }

                if (nextObject == null)
                {
                    nextObject = root.getBmObject();
                }
                else
                {
                    var newOwner = nextObject.eContainer();
                    if (newOwner == null)
                    {
                        nextObject = v8Model.getBmObjectOwner(root.getModel(), nextObject);
                    }
                    else
                    {
                        nextObject = newOwner;
                    }

                    if (nextObject == null)
                    {
                        break;
                    }
                }

                if (nextObject instanceof Module)
                {
                    visitor.visitModule(root, (Module)nextObject);
                    var contentsIterator = nextObject.eAllContents();
                    while (contentsIterator.hasNext())
                    {
                        if (cancellationToken.isCanceled())
                        {
                            break;
                        }

                        var obj = contentsIterator.next();
                        var node = v8Model.getNode(obj);
                        if (node == null)
                        {
                            continue;
                        }

                        visitor.visitNode(root, obj, node);
                        if (obj instanceof Variable || obj instanceof Invocation || obj instanceof FeatureAccess
                            || obj instanceof Method)
                        {
                            var nodeStart = node.getTotalOffset();
                            var nodeFinish = node.getTotalEndOffset();

                            if (!((nodeStart >= start && nodeStart <= finish)
                                || (nodeFinish >= start && nodeFinish <= finish)) && !(obj instanceof Method))
                            {
                                continue;
                            }

                            var nodeId = idFactory.createNodeId(path, node);
                            if (nodeId == null)
                            {
                                continue;
                            }

                            if (obj instanceof Method && visitor.visitMethod(root, nodeId, (Method)obj, node))
                            {
                                traceVisitEObject(obj, true);
                                return true;
                            }

                            if (!cancellationToken.isCanceled())
                            {
                                if (obj instanceof Variable && visitor.visitVariable(root, nodeId, (Variable)obj, node))
                                {
                                    traceVisitEObject(obj, true);
                                    return true;
                                }

                                if (obj instanceof Invocation
                                    && visitor.visitInvocation(root, nodeId, (Invocation)obj, node))
                                {
                                    traceVisitEObject(obj, true);
                                    return true;
                                }

                                if (obj instanceof FeatureAccess
                                    && visitor.visitFeatureAccess(root, nodeId, (FeatureAccess)obj, node))
                                {
                                    traceVisitEObject(obj, true);
                                    return true;
                                }
                            }
                        }

                        traceVisitEObject(obj, false);
                    }

                    continue;
                }

                if (nextObject instanceof Form)
                {
                    if (visitor.visitForm(root, (Form)nextObject))
                    {
                        return true;
                    }

                    continue;
                }

                if (!(nextObject instanceof IBmObject))
                {
                    continue;
                }

                var bmObject = (IBmObject)nextObject;
                if (visitor.visitBmObject(root, bmObject))
                {
                    return true;
                }
            }
        }
        catch (Exception error)
        {
            log.logError(error);
            return false;
        }

        return true;
    }

    private void traceVisitEObject(EObject eObject, boolean visited)
    {
        /*System.out.println(visited + ", " + eObject.getClass().getName() + ": "
            + getNode(eObject).getText().replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"));*/
    }
}
