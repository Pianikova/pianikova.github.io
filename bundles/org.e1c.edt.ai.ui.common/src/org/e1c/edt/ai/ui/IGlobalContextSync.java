/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.List;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.assistent.model.EntityKey;
import org.e1c.edt.ai.assistent.model.EntityValue;

interface IGlobalContextSync
{
    boolean sync(AIContext aiCtx, int maxDept, ICancellationToken cancellationToken);

    boolean sync(AIContext aiCtx, List<EntityValue> unknownValues, List<EntityKey> unknownKeys, int maxDept,
        ICancellationToken cancellationToken);
}
