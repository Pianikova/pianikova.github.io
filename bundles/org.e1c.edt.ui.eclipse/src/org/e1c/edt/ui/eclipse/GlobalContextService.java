package org.e1c.edt.ui.eclipse;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.assistent.IGlobalContextService;
import org.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import org.e1c.edt.ai.assistent.model.GlobalContextUpdateResponse;
import org.e1c.edt.ai.assistent.model.ProjectId;

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
