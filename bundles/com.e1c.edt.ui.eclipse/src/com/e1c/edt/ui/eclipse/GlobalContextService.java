package com.e1c.edt.ui.eclipse;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.assistent.IGlobalContextService;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdateResponse;
import com.e1c.edt.ai.assistent.model.ProjectId;

/**
 * Copyright (C) 2025, 1C
 */

class GlobalContextService
    implements IGlobalContextService
{
    @Override
    public CompletableFuture<Optional<GlobalContextUpdateResponse>> update(ProjectId projectId,
        Collection<GlobalContextUpdate> updates, IStatistics statistics, ICancellationToken cancellationToken)
    {
        return CompletableFuture.completedFuture(Optional.empty());
    }
}
