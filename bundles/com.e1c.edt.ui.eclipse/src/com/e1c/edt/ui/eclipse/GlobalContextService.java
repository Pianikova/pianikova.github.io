package com.e1c.edt.ui.eclipse;
import java.util.Collection;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.assistent.IGlobalContextService;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdateResponse;

class GlobalContextService
    implements IGlobalContextService
{
    @Override
    public CompletableFuture<Optional<GlobalContextUpdateResponse>> update(IProject project,
        Collection<GlobalContextUpdate> updates, int partitionSize, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        return CompletableFuture.completedFuture(Optional.empty());
    }
}
