/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.e1c.edt.ai.Range;
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

public class CodePartsProvider implements ICodePartsProvider
{
    @Override
    @SuppressWarnings("nls")
    public Iterable<CodePart> getParts(ICompositeNode rootNode)
    {
        var result = new ArrayList<CodePart>();
        var nodes = new ArrayList<ILeafNode>();
        var start = 0;
        CodePartType lastType = CodePartType.Unknown;
        var methodStarted = false;
        var argsStarted = false;
        var hasArgs = false;
        ILeafNode lastLeafNode = null;
        for (var leafNode : rootNode.getLeafNodes())
        {
            lastLeafNode = leafNode;
            var text = leafNode.getText();
            var type = CodePartType.Unknown;
            var semantic = NodeModelUtils.findActualSemanticObjectFor(leafNode);
            if (semantic != null)
            {
                var method = EcoreUtil2.getContainerOfType(semantic, Method.class);
                if (method != null)
                {
                    type = methodStarted ? CodePartType.Method : CodePartType.MethodPrefix;
                    var grammar = leafNode.getGrammarElement();
                    if (!methodStarted && grammar instanceof TerminalRule)
                    {
                        var terminalRule = (TerminalRule)grammar;
                        var name = terminalRule.getName();
                        if ("SL_COMMENT".equals(name))
                        {
                            type = CodePartType.Comment;
                        }
                    }

                    if (grammar instanceof Keyword)
                    {
                        if (!methodStarted)
                        {
                            if (getAlternatives(grammar).filter(i -> i instanceof Keyword)
                                .map(i -> (Keyword)i)
                                .anyMatch(i -> "Процедура".equalsIgnoreCase(i.getValue())))
                            {
                                type = CodePartType.Method;
                                methodStarted = true;
                                argsStarted = false;
                                hasArgs = false;
                                continue;
                            }
                        }
                        else
                        {
                            if (getAlternatives(grammar).filter(i -> i instanceof Keyword)
                                .map(i -> (Keyword)i)
                                .anyMatch(i -> "КонецПроцедуры".equalsIgnoreCase(i.getValue())))
                            {
                                methodStarted = false;
                                continue;
                            }

                            if (!hasArgs)
                            {
                                var keyword = (Keyword)grammar;
                                if (!argsStarted)
                                {
                                    if ("(".equals(keyword.getValue()))
                                    {
                                        type = CodePartType.MethodArgs;
                                        argsStarted = true;
                                    }
                                }
                                else
                                {
                                    if (")".equals(keyword.getValue()))
                                    {
                                        type = CodePartType.Method;
                                        argsStarted = false;
                                        hasArgs = true;
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (lastType != type)
            {
                var end = leafNode.getOffset();
                if (end > 0)
                {
                    result.add(new CodePart(new Range(start, end - start), lastType, nodes));
                    start = end;
                    nodes = new ArrayList<>();
                }
                lastType = type;
            }

            nodes.add(leafNode);
        }

        if (lastLeafNode != null)
        {
            var end = lastLeafNode.getEndOffset();
            result.add(new CodePart(new Range(start, end + 1 - start), lastType, nodes));
        }

        return result;
    }

    private Stream<AbstractElement> getAlternatives(EObject obj)
    {
        var сontainer = obj.eContainer();
        if (сontainer instanceof Alternatives)
        {
            return ((Alternatives)сontainer).getElements().stream();
        }

        return Stream.empty();
    }
}
