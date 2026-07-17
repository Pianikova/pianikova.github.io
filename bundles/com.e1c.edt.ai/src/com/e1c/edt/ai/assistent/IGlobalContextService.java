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
import org.eclipse.core.resources.IProject;

public interface IGlobalContextService
{
    CompletableFuture<Optional<GlobalContextUpdateResponse>> update(IProject project,
        Collection<GlobalContextUpdate> updates,
        int partitionSize,
        IStatistics statistics, ICancellationToken cancellationToken);
}
