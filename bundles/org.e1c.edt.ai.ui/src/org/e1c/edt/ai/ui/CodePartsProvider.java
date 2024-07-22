/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Iterator;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.Stack;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

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
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com._1c.g5.v8.dt.bsl.model.Method;
import com.google.common.base.Preconditions;

public class CodePartsProvider implements ICodePartsProvider
{
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
        private boolean beforeArgs;

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
            CodePart codePart = new CodePart(marker.range, CursorLocation.OutsideFunction);
            switch (marker.type)
            {
            case Comment:
                if (getLastLocation() == CursorLocation.FunctionBody)
                {
                    codePart = new CodePart(marker.range, CursorLocation.FunctionBody);
                    break;
                }

                codePart = new CodePart(marker.range, CursorLocation.Comment);
                break;

            case MethodStart:
                beforeArgs = true;
                locations.push(CursorLocation.FunctionBody);
                codePart = new CodePart(marker.range, CursorLocation.FunctionBody);
                break;

            case MethodArgStart:
                if (getLastLocation() != CursorLocation.FunctionBody)
                {
                    break;
                }

                beforeArgs = false;
                locations.push(CursorLocation.FunctionArguments);
                codePart = new CodePart(marker.range, CursorLocation.FunctionArguments);
                break;

            case MethodArgFinish:
                popLocation();
                codePart = new CodePart(marker.range, CursorLocation.FunctionArguments);
                break;

            case MethodFinish:
                popLocation();
                codePart = new CodePart(marker.range, CursorLocation.FunctionBody);
                break;

            case Unknown:
                var lastLocation = getLastLocation();
                if (lastLocation == CursorLocation.FunctionBody && beforeArgs)
                {
                    lastLocation = CursorLocation.FunctionName;
                }

                codePart = new CodePart(marker.range, lastLocation);
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
                return new CodeMarker(range, MarkerType.Unknown);
            }

            var method = EcoreUtil2.getContainerOfType(semantic, Method.class);
            if (method == null)
            {
                return new CodeMarker(range, MarkerType.Unknown);
            }

            var grammar = leafNode.getGrammarElement();
            if (grammar instanceof TerminalRule)
            {
                var terminalRule = (TerminalRule)grammar;
                var name = terminalRule.getName();
                if ("SL_COMMENT".equals(name))
               {
                    return new CodeMarker(range, MarkerType.Comment);
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
                    return new CodeMarker(range, MarkerType.MethodStart);
               }

                if (getAlternatives(grammar).filter(i -> i instanceof Keyword)
                    .map(i -> (Keyword)i)
                    .anyMatch(i -> "КонецПроцедуры".equalsIgnoreCase(i.getValue())
                        || "КонецФункции".equalsIgnoreCase(i.getValue())))
               {
                    return new CodeMarker(range, MarkerType.MethodFinish);
               }

                if ("(".equals(keyword.getValue()))
               {
                    return new CodeMarker(range, MarkerType.MethodArgStart);
                }

                if (")".equals(keyword.getValue()))
                {
                    return new CodeMarker(range, MarkerType.MethodArgFinish);
               }
            }

            return new CodeMarker(range, MarkerType.Unknown);
        });
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
        public final Range range;
        public final MarkerType type;

        public CodeMarker(Range range, MarkerType type)
        {
            Preconditions.checkNotNull(range);
            Preconditions.checkNotNull(type);
            this.range = range;
            this.type = type;
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
