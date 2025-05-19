/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.HashSet;
import java.util.List;

import com.e1c.edt.ai.assistent.model.GlobalContext;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.ProjectId;

public interface IGlobalContextRequestFactory
{
    List<GlobalContextUpdate> createGlobalContextUpdates(String filePath,
        GlobalContext globalContext, IStatistics statistics, ICancellationToken cancellationToken);

    List<GlobalContextUpdate> createGlobalContextUpdates(ProjectId projectId, String filePath,
        HashSet<String> hashes,
        HashSet<String> fields,
        IStatistics statistics,
        ICancellationToken cancellationToken);
}