/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.stream.Stream;

import org.eclipse.xtext.nodemodel.ICompositeNode;

public interface ICodePartsProvider
{
    Stream<CodePart> getParts(ICompositeNode rootNode);
}
