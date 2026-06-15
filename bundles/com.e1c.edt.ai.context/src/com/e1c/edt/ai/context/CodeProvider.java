/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.Optional;

import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.parser.IParseResult;

import com._1c.g5.v8.dt.bsl.model.Method;
import com.e1c.edt.ai.CodeMethod;
import com.e1c.edt.ai.ICodeProvider;

class CodeProvider
    implements ICodeProvider
{
    @Override
    public Optional<CodeMethod> getMethod(IParseResult parseResult, int offset)
    {
        if (parseResult == null)
        {
            return Optional.empty();
        }

        for (var error : parseResult.getSyntaxErrors())
        {
            if (error.getTotalEndOffset() < offset)
            {
                return Optional.empty();
            }
        }

        var rootNode = parseResult.getRootNode();
        if (rootNode == null)
        {
            return Optional.empty();
        }

        var cursorNode = NodeModelUtils.findLeafNodeAtOffset(rootNode, offset);
        if (cursorNode == null)
        {
            return Optional.empty();
        }

        var semantic = NodeModelUtils.findActualSemanticObjectFor(cursorNode);
        if (semantic == null)
        {
            return Optional.empty();
        }

        var method = EcoreUtil2.getContainerOfType(semantic, Method.class);
        if (method == null)
        {
            return Optional.empty();
        }

        var uniqueName = method.getUniqueName();
        if (uniqueName == null)
        {
            return Optional.empty();
        }

        var methodNode = NodeModelUtils.getNode(method);
        if (methodNode == null)
        {
            return Optional.empty();
        }

        var startOffest = methodNode.getTotalOffset();
        var endOffest = methodNode.getTotalEndOffset();
        return Optional.of(new CodeMethod(uniqueName, startOffest, endOffest, Optional.of(parseResult)));
    }

    @Override
    public Optional<String> getMethodBody(IParseResult parseResult, CodeMethod method)
    {
        var rootNode = parseResult.getRootNode();
        if (rootNode == null)
        {
            return Optional.empty();
        }

        var rootSemantic = NodeModelUtils.findActualSemanticObjectFor(rootNode);
        if (rootSemantic == null)
        {
            return Optional.empty();
        }

        for (var curMethod : EcoreUtil2.getAllContentsOfType(rootSemantic, Method.class))
        {
            if (method.getUniqueName().equals(curMethod.getUniqueName()))
            {
                var methodNode = NodeModelUtils.getNode(curMethod);
                if (methodNode == null)
                {
                    break;
                }

                var sb = new StringBuilder();
                for (var leafNode : methodNode.getLeafNodes())
                {
                    sb.append(leafNode.getText());
                }

                return Optional.of(sb.toString());
            }
        }

        return Optional.empty();
    }
}
