/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ui.eclipse;

import java.util.stream.Stream;

import com.e1c.edt.ai.CodePart;
import com.e1c.edt.ai.ICodePartsProvider;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;

class CodePartsProvider
    implements ICodePartsProvider
{
    @Override
    public boolean isMethod(INode node)
    {
        return false;
    }

    @Override
    public Stream<CodePart> getParts(ICompositeNode rootNode)
    {
        return Stream.empty();
    }
}
