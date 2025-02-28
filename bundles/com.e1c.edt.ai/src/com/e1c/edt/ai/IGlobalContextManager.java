/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.model.Completion;

public interface IGlobalContextManager
{
    void update(AIContext aiCtx, ICancellationToken cancellationToken);

    void update(AIContext aiCtx, Completion completion, ICancellationToken cancellationToken);
}
