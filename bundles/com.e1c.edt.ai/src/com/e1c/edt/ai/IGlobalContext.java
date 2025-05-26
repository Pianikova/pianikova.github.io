/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.HashSet;
import java.util.List;

import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.ProjectId;

public interface IGlobalContext
{
    List<GlobalContextUpdate> getUpdates(AIContext aiContext, boolean sendInitialState, IStatistics statistics,
        ICancellationToken cancellationToken);

    List<GlobalContextUpdate> getUpdates(ProjectId projectId, String filePath, String fileHash, HashSet<String> hashes,
        HashSet<String> fields, IStatistics statistics, ICancellationToken cancellationToken);
}