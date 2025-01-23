/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.assistent.model.EntityKey;
import org.e1c.edt.ai.assistent.model.EntityValue;
import org.e1c.edt.ai.assistent.model.GlobalContextUpdate;

interface IGlobalContextSync
{
    CompletableFuture<Boolean> sync(AIContext aiCtx, int maxDept, ICancellationToken cancellationToken);

    List<GlobalContextUpdate> getSyncData(AIContext aiCtx, IStatistics statistics,
        ICancellationToken cancellationToken);

    public CompletableFuture<Boolean> sync(AIContext aiCtx, List<GlobalContextUpdate> updates, int maxDept,
        IStatistics statistics, ICancellationToken cancellationToken);

    boolean sync(AIContext aiCtx, List<EntityValue> unknownValues, List<EntityKey> unknownKeys, int maxDept,
        ICancellationToken cancellationToken);
}
