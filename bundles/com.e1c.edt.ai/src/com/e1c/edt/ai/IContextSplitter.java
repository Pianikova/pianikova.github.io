/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import org.eclipse.core.resources.IProject;

public interface IContextSplitter
{
    ContextParts split(IProject project, String text, int offset, boolean limitSize);
}
