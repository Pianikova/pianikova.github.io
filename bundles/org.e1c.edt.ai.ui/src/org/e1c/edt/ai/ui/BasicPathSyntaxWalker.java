/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import org.e1c.edt.ai.CancellationToken;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.util.ITextRegion;

import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Statement;
import com._1c.g5.v8.dt.bsl.model.impl.IfStatementImpl;
import com.google.common.base.Preconditions;

public class BasicPathSyntaxWalker<TContext>
    implements ISyntaxWalker<TContext>
{
    @Override
    public void walk(INode targetNode, ISyntaxVisitor<TContext> visitor, SyntaxWalkerContext<TContext> ctx,
        CancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(targetNode);
        Preconditions.checkNotNull(visitor);
        Preconditions.checkNotNull(ctx);
        var cursorNode = ctx.getCursorNode();
        var settings = ctx.getSourceCtx();
        var cursorSemantic = NodeModelUtils.findActualSemanticObjectFor(cursorNode);
        var cursorMethod = EcoreUtil2.getContainerOfType(cursorSemantic, Method.class);
        var excludingRegionsByMethod = new HashMap<EObject, Collection<ITextRegion>>();
        var excludingMethods = new HashSet<Method>();
        for (var node : targetNode.getAsTreeIterable())
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            var semantic = NodeModelUtils.findActualSemanticObjectFor(node);
            if (semantic != null)
            {
                var method = EcoreUtil2.getContainerOfType(semantic, Method.class);
                if (method != null)
                {
                    if (excludingMethods.contains(method))
                    {
                        continue;
                    }

                    if (!excludingRegionsByMethod.containsKey(method))
                    {
                        var excluding = new ArrayList<EObject>();
                        if (method == cursorMethod)
                        {
                            var path = getPath(cursorSemantic);
                            var skipMethodTail = settings.SkipMethodTail;
                            var skipStatemntsOutOfStack = settings.SkipOutOfStackStatements;
                            fillExcludingStatements(skipMethodTail, skipStatemntsOutOfStack, method.allStatements(),
                                path, excluding);
                        }
                        else
                        {
                            if (settings.SkipMinorMethods)
                            {
                                excludingMethods.add(method);
                            }
                            else
                            {
                                if (settings.SkipMinorMethodStatements)
                                {
                                    excluding.addAll(method.allStatements());
                                }
                            }
                        }

                        var excludingRegions = new ArrayList<ITextRegion>();
                        for (var excludingItem : excluding)
                        {
                            fillRegions(excludingItem, excludingRegions);
                        }

                        excludingRegionsByMethod.put(method, excludingRegions);
                    }
                }
            }

            if (excludingRegionsByMethod.values()
                .stream()
                .filter(regions -> regions.stream()
                    .filter(region -> region.contains(node.getOffset()))
                    .findAny()
                    .isPresent())
                .findAny()
                .isPresent())
            {
                continue;
            }

            if (!visitor.visitNode(node, ctx.getCtx()))
            {
                break;
            }
        }
    }

    private HashSet<EObject> getPath(EObject target)
    {
        EObject container = target;
        var path = new HashSet<EObject>();
        while (container != null)
        {
            if (container instanceof Statement)
            {
                path.add(container);
            }

            container = container.eContainer();
        }
        return path;
    }

    private void fillExcludingStatements(boolean skipMethodTail, boolean skipStatemntsOutOfStack,
        EList<Statement> statements, HashSet<EObject> path,
        Collection<EObject> excludingStatemnts)
    {
        var hasStatementInPath = false;
        for (var statement : statements)
        {
            if (hasStatementInPath && skipMethodTail)
            {
                excludingStatemnts.add(statement);
                continue;
            }

            hasStatementInPath = path.contains(statement);
            if (skipStatemntsOutOfStack)
            {
                if (statement instanceof IfStatementImpl)
                {
                    if (hasStatementInPath)
                    {
                        var ifStatement = ((IfStatementImpl)statement);
                        fillExcludingStatements(false, skipStatemntsOutOfStack, ifStatement.getIfPart().getStatements(),
                            path, excludingStatemnts);
                        fillExcludingStatements(false, skipStatemntsOutOfStack, ifStatement.getElseStatements(), path,
                            excludingStatemnts);

                        continue;
                    }
                    else
                    {
                        excludingStatemnts.add(statement);
                        continue;
                    }
                }
            }
        }
    }

    private void fillRegions(EObject target, Collection<ITextRegion> regions)
    {
        var leafNodeIterator = NodeModelUtils.getNode(target).getLeafNodes().iterator();
        if (!leafNodeIterator.hasNext())
        {
            return;
        }

        var firstLeaf = leafNodeIterator.next();
        var parent = target.eContainer();
        var start = false;
        ITextRegion region = null;
        for (var leafNode : NodeModelUtils.getNode(parent).getLeafNodes())
        {
            if (!start)
            {
                if (leafNode == firstLeaf)
                {
                    start = true;
                }

                continue;
            }

            if (region == null)
            {
                region = leafNode.getTextRegion();
            }
            else
            {
                region = region.merge(leafNode.getTextRegion());
            }

            if (NodeModelUtils.findActualSemanticObjectFor(leafNode) == parent)
            {
                break;
            }
        }

        if (region != null)
        {
            regions.add(region);
        }
    }
}
