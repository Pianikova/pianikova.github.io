/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.time.Duration;
import java.util.function.Predicate;

import com.e1c.edt.ai.assistent.model.ChatContext;
import com.e1c.edt.ai.assistent.model.GlobalContext;
import com.e1c.edt.ai.assistent.model.LocalContext;

public interface IContextEntities
{
    Duration fill(AIContext aiContext, LocalContext localContext, GlobalContext globalContext,
        Predicate<FillAction> actionFilter, IStatistics statistics,
        ICancellationToken cancellationToken);

    void fill(AIContext aiContext, ChatContext chatContext, IStatistics statistics,
        ICancellationToken cancellationToken);
}
