/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.assistent.model.CompletionRequest;

public interface ICompletionRequestProvider
{
    public Optional<CompletionRequest> get(IStatistics statistics, ICancellationToken cancellationToken);
}
