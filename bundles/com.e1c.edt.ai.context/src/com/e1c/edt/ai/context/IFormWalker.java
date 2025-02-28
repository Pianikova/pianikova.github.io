/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import com.e1c.edt.ai.ICancellationToken;
import org.eclipse.emf.ecore.EObject;

interface IFormWalker
{
    void walk(EObject root, IFormVisitor visitor, ICancellationToken cancellationToken);
}
