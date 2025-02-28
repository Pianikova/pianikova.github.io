/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.stream.Stream;

import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;

public interface ICodePartsProvider
{
    boolean isMethod(INode node);

    Stream<CodePart> getParts(ICompositeNode rootNode);
}
