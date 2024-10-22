/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.CodeMethod;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.EcoreUtil2;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;

import com._1c.g5.v8.dt.bsl.model.Method;

public class CodeProvider implements ICodeProvider
{
    @Override
    public Optional<IParseResult> getParseResult(SourceViewer sourceViewer)
    {
        var doc = sourceViewer.getDocument();
        if (!(doc instanceof IXtextDocument))
        {
            return Optional.empty();
        }

        return Optional.ofNullable(((IXtextDocument)doc).readOnly(s -> s.getParseResult()));
    }

    @Override
    public Optional<CodeMethod> getMethod(IParseResult parseResult, int offset)
    {
        if (parseResult == null)
        {
            return Optional.empty();
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

        return Optional.of(new CodeMethod(method.getUniqueName()));
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
            if (curMethod.getUniqueName().equals(method.getUniqueName()))
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
