/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ui.eclipse;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IGlobalContextManager;
import com.e1c.edt.ai.assistent.model.Completion;

class GlobalContextManager
    implements IGlobalContextManager
{
    @Override
    public void update(AIContext aiCtx, ICancellationToken cancellationToken)
    {
        //
    }

    @Override
    public void update(AIContext aiCtx, Completion completion, ICancellationToken cancellationToken)
    {
        //
    }
}
