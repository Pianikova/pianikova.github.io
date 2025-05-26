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
import com.e1c.edt.ai.assistent.model.ProjectId;

interface IGlobalContextSync
{
    CompletableFuture<Boolean> sync(AIContext aiContext, int maxDept,
        ICancellationToken cancellationToken);

    CompletableFuture<Boolean> syncUpdates(ProjectId projectId, boolean isInitial, List<GlobalContextUpdate> updates,
        int maxDept,
        IStatistics statistics, ICancellationToken cancellationToken);

    CompletableFuture<Boolean> syncUnknown(ProjectId projectId, boolean isInitial, List<EntityValue> unknownValues,
        List<EntityKey> unknownKeys,
        int maxDept,
        ICancellationToken cancellationToken);
}
