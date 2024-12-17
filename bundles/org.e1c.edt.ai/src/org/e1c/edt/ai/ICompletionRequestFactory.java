/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import org.e1c.edt.ai.assistent.model.CompletionRequest;

public interface ICompletionRequestFactory
{
    CompletionRequest createCompletion(AIContext aiContext, IStatistics statistics,
        ICancellationToken cancellationToken);
}
