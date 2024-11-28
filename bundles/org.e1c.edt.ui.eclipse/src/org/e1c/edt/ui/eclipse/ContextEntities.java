/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ui.eclipse;

import java.time.Duration;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IContextEntities;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.assistent.model.ChatContext;
import org.e1c.edt.ai.assistent.model.LocalContext;

class ContextEntities
    implements IContextEntities
{
    @Override
    public Duration fill(AIContext aiContext, LocalContext context, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        return Duration.ZERO;
    }

    @Override
    public void fill(AIContext aiContext, ChatContext context, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        //
    }
}
