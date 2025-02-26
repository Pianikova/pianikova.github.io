/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai;

import java.util.HashSet;
import java.util.List;

import org.e1c.edt.ai.assistent.model.GlobalContext;
import org.e1c.edt.ai.assistent.model.GlobalContextUpdate;

public interface IGlobalContextRequestFactory
{
    List<GlobalContextUpdate> createGlobalContextUpdates(AIContext aiContext,
        GlobalContext globalContext, IStatistics statistics, ICancellationToken cancellationToken);

    List<GlobalContextUpdate> createGlobalContextUpdates(AIContext aiContext,
        HashSet<String> hashes,
        HashSet<String> fields,
        IStatistics statistics,
        ICancellationToken cancellationToken);
}