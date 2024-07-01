/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.ArrayList;

import org.e1c.edt.ai.Range;
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
        Method lastMethod = null;
        var methodStarted = false;
        ILeafNode lastLeafNode = null;
        for (var leafNode : rootNode.getLeafNodes())
        {
            lastLeafNode = leafNode;
            var type = CodePartType.Unknown;
            var semantic = NodeModelUtils.findActualSemanticObjectFor(leafNode);
            if (semantic != null)
            {
                var method = EcoreUtil2.getContainerOfType(semantic, Method.class);
                if (method != null)
                {
                    if (lastMethod != method)
                    {
                        lastMethod = method;
                        methodStarted = false;
                    }

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

                    if (!methodStarted && grammar instanceof Keyword)
                    {
                        type = CodePartType.Method;
                        methodStarted = true;
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
}
