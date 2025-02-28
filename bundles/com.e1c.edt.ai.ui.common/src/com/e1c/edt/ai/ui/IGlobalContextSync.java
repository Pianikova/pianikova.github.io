/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.assistent.model.EntityKey;
import com.e1c.edt.ai.assistent.model.EntityValue;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;

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
