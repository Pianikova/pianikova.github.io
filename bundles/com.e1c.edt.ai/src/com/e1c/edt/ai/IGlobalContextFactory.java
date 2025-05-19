/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.model.GlobalContext;
import com.e1c.edt.ai.assistent.model.ProjectId;

public interface IGlobalContextFactory
{
    GlobalContext createGlobalContext(ProjectId projectId, String filePath, IStatistics statistics,
        ICancellationToken cancellationToken);
}
