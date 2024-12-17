/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.assistent.model.Completion;

interface IGlobalContextViewModel
{
    void registerCompletion(AIContext aiCtx, Completion completion, ICancellationToken cancellationToken);
}
