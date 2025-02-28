/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.Optional;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.assistent.model.CompletionRequest;

public interface ICompletionRequestProvider
{
    public Optional<CompletionRequest> get(IStatistics statistics, ICancellationToken cancellationToken);
}
