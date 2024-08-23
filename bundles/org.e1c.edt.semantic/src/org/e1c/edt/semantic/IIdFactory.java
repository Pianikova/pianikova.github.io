/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

public interface IIdFactory
{
    String createNodeId(String path, ICompositeNode node);

    String createObjectId(String path, EObject eObject);

    Optional<SourceSpan> paeNodeId(String nodeId);
}
