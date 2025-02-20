/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Stack;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import org.e1c.edt.ai.CodePart;
import org.e1c.edt.ai.ICodePartsProvider;
import org.e1c.edt.ai.Range;
import org.e1c.edt.ai.assistent.model.CursorLocation;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.AbstractElement;
import org.eclipse.xtext.Alternatives;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.Keyword;
import org.eclipse.xtext.TerminalRule;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.ILeafNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.nodemodel.impl.CompositeNodeWithSemanticElement;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Function;
import com._1c.g5.v8.dt.bsl.model.Method;
import com.google.common.base.Preconditions;

class CodePartsProvider
    implements ICodePartsProvider
{
    @Override
    public boolean isMethod(INode node)
    {
        var semantic = NodeModelUtils.findActualSemanticObjectFor(node);
        if (semantic == null)
        {
            return false;
        }

        return EcoreUtil2.getContainerOfType(semantic, Method.class) != null;
    }

    @Override
    public Stream<CodePart> getParts(ICompositeNode rootNode)
    {
        Preconditions.checkNotNull(rootNode);
        var leafNodes = StreamSupport.stream(
            Spliterators.spliteratorUnknownSize(rootNode.getLeafNodes().iterator(), Spliterator.IMMUTABLE), false);
        var markers = getMarkers(leafNodes);
        var codePartIterator = new CodePartIterator(markers);
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(codePartIterator, Spliterator.IMMUTABLE),
            false);
    }

    private class CodePartIterator
        implements Iterator<CodePart>
    {
        private final Iterator<CodeMarker> markers;
        private final Stack<CursorLocation> locations = new Stack<>();
        private final HashSet<Method> methods = new HashSet<>();
        private boolean beforeArgs;
        private int lastMethodId;

        public CodePartIterator(Stream<CodeMarker> markers)
        {
            Preconditions.checkNotNull(markers);
            this.markers = markers.iterator();
        }

        @Override
        public boolean hasNext()
        {
            return markers.hasNext();
        }

        @Override
        public CodePart next()
        {
            var marker = markers.next();
            var method = marker.method;
            Integer methodId = null;
            if (method != null)
            {
                if (methods.add(method))
                {
                    lastMethodId++;
                    beforeArgs = true;
                }

                methodId = lastMethodId;
            }

            CodePart codePart = new CodePart(methodId, marker.range, CursorLocation.OutsideFunction, marker.text);
            switch (marker.type)
            {
            case Comment:
                if (getLastLocation() == CursorLocation.FunctionBody)
                {
                    codePart = new CodePart(methodId, marker.range, CursorLocation.FunctionBody, marker.text);
                    break;
                }

                codePart = new CodePart(methodId, marker.range, CursorLocation.Comment, marker.text);
                break;

            case MethodStart:
                locations.push(CursorLocation.FunctionBody);
                codePart = new CodePart(methodId, marker.range, CursorLocation.FunctionBody, marker.text);
                break;

            case MethodArgStart:
                if (getLastLocation() != CursorLocation.FunctionBody)
                {
                    break;
                }

                locations.push(CursorLocation.FunctionArguments);
                codePart = new CodePart(methodId, marker.range, CursorLocation.FunctionArguments, marker.text);
                beforeArgs = false;
                break;

            case MethodArgFinish:
                popLocation();
                codePart = new CodePart(methodId, marker.range, CursorLocation.FunctionArguments, marker.text);
                break;

            case MethodFinish:
                popLocation();
                codePart = new CodePart(methodId, marker.range, CursorLocation.FunctionBody, marker.text);
                break;

            case Unknown:
                var lastLocation = getLastLocation();
                if (lastLocation == CursorLocation.FunctionBody && beforeArgs)
                {
                    lastLocation = CursorLocation.FunctionName;
                }

                codePart = new CodePart(methodId, marker.range, lastLocation, marker.text);
                break;

            default:
                break;
            }

            return codePart;
        }

        private void popLocation()
        {
            if (locations.size() > 0)
            {
                locations.pop();
            }
        }

        private CursorLocation getLastLocation()
        {
            if (locations.size() > 0)
            {
                return locations.peek();
            }

            return CursorLocation.OutsideFunction;
        }
    }

    @SuppressWarnings("nls")
    private Stream<CodeMarker> getMarkers(Stream<ILeafNode> leafNodes)
    {
        return leafNodes.map(leafNode -> {
            var text = leafNode.getText();
            var range = new Range(leafNode.getTotalOffset(), leafNode.getLength());
            var semantic = NodeModelUtils.findActualSemanticObjectFor(leafNode);
            if (semantic == null)
            {
                return new CodeMarker(null, range, MarkerType.Unknown, text);
            }

            var method = EcoreUtil2.getContainerOfType(semantic, Method.class);
            if (method == null)
            {
                return new CodeMarker(method, range, MarkerType.Unknown, text);
            }

            var grammar = leafNode.getGrammarElement();
            if (grammar instanceof TerminalRule)
            {
                var terminalRule = (TerminalRule)grammar;
                var name = terminalRule.getName();
                if ("SL_COMMENT".equals(name))
               {
                    return new CodeMarker(method, range, MarkerType.Comment, text);
               }
            }

            if (grammar instanceof Keyword)
            {
                var keyword = (Keyword)grammar;

                if (getAlternatives(grammar).filter(i -> i instanceof Keyword)
                    .map(i -> (Keyword)i)
                    .anyMatch(
                        i -> "Процедура".equalsIgnoreCase(i.getValue()) || "Функция".equalsIgnoreCase(i.getValue())))
               {
                    return new CodeMarker(method, range, MarkerType.MethodStart, text);
               }

                if (getAlternatives(grammar).filter(i -> i instanceof Keyword)
                    .map(i -> (Keyword)i)
                    .anyMatch(i -> "КонецПроцедуры".equalsIgnoreCase(i.getValue())
                        || "КонецФункции".equalsIgnoreCase(i.getValue())))
               {
                    return new CodeMarker(method, range, MarkerType.MethodFinish, text);
               }

                if (isArgRelated(leafNode) && "(".equals(keyword.getValue()))
               {
                    return new CodeMarker(method, range, MarkerType.MethodArgStart, text);
                }

                if (isArgRelated(leafNode) && ")".equals(keyword.getValue()))
                {
                    return new CodeMarker(method, range, MarkerType.MethodArgFinish, text);
               }
            }

            return new CodeMarker(method, range, MarkerType.Unknown, text);
        });
    }

    private boolean isArgRelated(ILeafNode node)
    {
        var parent = node.getParent();
        if(parent == null)
        {
            return false;
        }

        if (!(parent instanceof CompositeNodeWithSemanticElement))
        {
            return false;
        }

        var parentWithSemantic = (CompositeNodeWithSemanticElement)parent;
        var semanticElement = parentWithSemantic.getSemanticElement();
        return semanticElement instanceof Method || semanticElement instanceof Function;
    }

    private Stream<AbstractElement> getAlternatives(EObject obj)
    {
        Preconditions.checkNotNull(obj);
        var сontainer = obj.eContainer();
        if (сontainer instanceof Alternatives)
        {
            return ((Alternatives)сontainer).getElements().stream();
        }

        return Stream.empty();
    }

    private class CodeMarker
    {
        private final Method method;
        public final Range range;
        public final MarkerType type;
        public final String text;

        public CodeMarker(Method method, Range range, MarkerType type, String text)
        {
            Preconditions.checkNotNull(range);
            Preconditions.checkNotNull(type);
            Preconditions.checkNotNull(text);
            this.method = method;
            this.range = range;
            this.type = type;
            this.text = text;
        }

        @SuppressWarnings("nls")
        @Override
        public String toString()
        {
            return range.toString() + ": " + type;
        }
    }

    private enum MarkerType
    {
        Unknown,
        Comment,
        MethodStart,
        MethodFinish,
        MethodArgStart,
        MethodArgFinish
    }
}
