/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ui.eclipse;

import java.util.stream.Stream;

import org.e1c.edt.ai.CodePart;
import org.e1c.edt.ai.ICodePartsProvider;
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
