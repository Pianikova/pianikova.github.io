/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IStatistics;

interface IEntitiesWalker
{
    boolean walk(String path, int start, int finish, IModuleProvider resourceSetProvider, IEntityVisitor visitor,
        IStatistics statistics,
        ICancellationToken cancellationToken);
}
