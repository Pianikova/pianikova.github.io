/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import org.e1c.edt.ai.assistent.model.GlobalContextUpdateResponse;

public interface IGlobalContextService
{
    CompletableFuture<Optional<GlobalContextUpdateResponse>> update(Collection<GlobalContextUpdate> updates,
        IStatistics statistics, ICancellationToken cancellationToken);
}
