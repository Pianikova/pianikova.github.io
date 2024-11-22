/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

interface IIdFactory
{
    String createNodeId(String path, ICompositeNode node);

    String createObjectId(String path, EObject eObject, ICancellationToken cancellationToken);

    Optional<SourceSpan> getNodeId(String nodeId);
}
