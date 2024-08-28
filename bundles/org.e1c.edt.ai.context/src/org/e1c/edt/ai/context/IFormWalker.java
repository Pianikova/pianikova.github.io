/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.e1c.edt.ai.ICancellationToken;
import org.eclipse.emf.ecore.EObject;

public interface IFormWalker
{
    void walk(EObject root, IFormVisitor visitor, ICancellationToken cancellationToken);
}
