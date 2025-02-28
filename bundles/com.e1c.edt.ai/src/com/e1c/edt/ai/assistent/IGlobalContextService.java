/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdateResponse;
import com.e1c.edt.ai.assistent.model.ProjectId;

public interface IGlobalContextService
{
    CompletableFuture<Optional<GlobalContextUpdateResponse>> update(ProjectId projectId,
        Collection<GlobalContextUpdate> updates,
        IStatistics statistics, ICancellationToken cancellationToken);
}
