/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.model.ProjectId;

public interface IContextSplitter
{
    ContextParts split(ProjectId projectId, String text, int offset);
}
