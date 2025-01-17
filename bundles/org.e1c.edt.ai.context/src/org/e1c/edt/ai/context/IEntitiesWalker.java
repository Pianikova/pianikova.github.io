/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IStatistics;

interface IEntitiesWalker
{
    boolean walk(String path, int start, int finish, IModuleProvider resourceSetProvider, IEntityVisitor visitor,
        IStatistics statistics,
        ICancellationToken cancellationToken);
}
