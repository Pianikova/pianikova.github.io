/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.net.MalformedURLException;
import java.util.List;

import org.e1c.edt.ai.ILog;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Variable;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class EntitiesWalker
    implements IEntitiesWalker
{
    private final ILog log;
    private final IV8Model v8Model;
    private final IIdFactory idFactory;

    @Inject
    public EntitiesWalker(ILog log, IV8Model v8Model, IIdFactory idFactory)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(v8Model);
        Preconditions.checkNotNull(idFactory);
        this.log = log;
        this.v8Model = v8Model;
        this.idFactory = idFactory;
    }

    @Override
    public boolean walk(String path, List<Integer> span, IEntityVisitor visitor)
    {
        var optionalModule = v8Model.getModule(path);
        if (optionalModule.isEmpty())
        {
            return false;
        }

        var module = optionalModule.get();
        var contentsIterator = module.eAllContents();
        while (contentsIterator.hasNext())
        {
            var obj = contentsIterator.next();
            if (obj instanceof Variable || obj instanceof Invocation || obj instanceof FeatureAccess)
            {
                var node = getNode(obj);
                var id = tryCreateId(path, span, node);
                if (id == null)
                {
                    continue;
                }

                if (obj instanceof Variable && visitor.visitVariable(id, (Variable)obj, node))
                {
                    traceVisit(obj, true);
                    return true;
                }

                if (obj instanceof Invocation && visitor.visitInvocation(id, (Invocation)obj, node))
                {
                    traceVisit(obj, true);
                    return true;
                }

                if (obj instanceof FeatureAccess && visitor.visitFeatureAccess(id, (FeatureAccess)obj, node))
                {
                    traceVisit(obj, true);
                    return true;
                }
            }

            traceVisit(obj, false);
        }

        return true;
    }

    @SuppressWarnings("nls")
    private void traceVisit(EObject eObject, boolean visited)
    {
        /*System.out.println(visited + ", " + eObject.getClass().getName() + ": "
            + getNode(eObject).getText().replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t"));*/
    }

    private ICompositeNode getNode(EObject eObject)
    {
        var obj = eObject;
        while (obj != null)
        {
            var node = NodeModelUtils.getNode(obj);
            if (node != null)
            {
                return node;
            }

            obj = obj.eContainer();
        }

        return null;
    }

    private String tryCreateId(String path, List<Integer> span, ICompositeNode node)
    {
        if (node == null)
        {
            return null;
        }

        var start = node.getTotalOffset();
        var finish = start + node.getTotalEndOffset();
        if (!isAccepting(span, start, finish))
        {
            return null;
        }

        try
        {
            return idFactory.create(path, node);
        }
        catch (MalformedURLException e)
        {
            log.logError(e);
            return null;
        }
    }

    private boolean isAccepting(List<Integer> span, int start, int finish)
    {
        if (span == null || span.isEmpty())
        {
            return true;
        }

        for (var i = 0; i < span.size() / 2; i += 2)
        {
            var startSpan = span.get(i);
            var finishSpan = span.get(i + 1);
            if ((start >= startSpan && start <= finishSpan) || (finish >= startSpan && finish <= finishSpan))
            {
                return true;
            }
        }

        return false;
    }
}
