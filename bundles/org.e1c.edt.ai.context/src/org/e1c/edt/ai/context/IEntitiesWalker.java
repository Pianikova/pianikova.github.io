/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.e1c.edt.ai.ICancellationToken;

public interface IEntitiesWalker
{
    boolean walk(String path, int start, int finish, IEntityVisitor visitor, ICancellationToken cancellationToken);
}
