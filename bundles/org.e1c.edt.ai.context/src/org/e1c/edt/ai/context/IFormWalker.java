/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.eclipse.emf.ecore.EObject;

public interface IFormWalker
{
    void walk(EObject root, IFormVisitor visitor);
}
