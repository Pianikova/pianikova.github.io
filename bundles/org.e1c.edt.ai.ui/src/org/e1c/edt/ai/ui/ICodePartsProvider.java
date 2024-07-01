/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.eclipse.xtext.nodemodel.ICompositeNode;

public interface ICodePartsProvider
{
    Iterable<CodePart> getParts(ICompositeNode rootNode);
}
