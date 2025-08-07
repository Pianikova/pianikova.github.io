/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.HashSet;
import java.util.List;

import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;

public interface IGlobalContext
{
    List<GlobalContextUpdate> getUpdates(AIContext aiContext, IStatistics statistics,
        ICancellationToken cancellationToken);

    List<GlobalContextUpdate> getUpdates(AIContext aiContext, String fileHash, HashSet<String> hashes,
        HashSet<String> fields, IStatistics statistics, ICancellationToken cancellationToken);
}