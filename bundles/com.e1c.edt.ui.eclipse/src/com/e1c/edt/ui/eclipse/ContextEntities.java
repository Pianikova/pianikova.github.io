/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ui.eclipse;

import java.time.Duration;
import java.util.function.Predicate;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.FillAction;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IContextEntities;
import com.e1c.edt.ai.IProgramingLanguage;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.assistent.model.ChatContext;
import com.e1c.edt.ai.assistent.model.GlobalContext;
import com.e1c.edt.ai.assistent.model.LocalContext;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class ContextEntities
    implements IContextEntities
{
    private final IProgramingLanguage programingLanguage;

    @Inject
    public ContextEntities(IProgramingLanguage programingLanguage)
    {
        Preconditions.checkNotNull(programingLanguage);
        this.programingLanguage = programingLanguage;
    }

    @Override
    public Duration fill(AIContext aiContext, LocalContext localContext, GlobalContext globalContext,
        Predicate<FillAction> actionFilter, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var filePath = aiContext.getPath();
        programingLanguage.getFromPath(filePath).ifPresent(lang -> localContext.programingLanguage = lang);
        return Duration.ZERO;
    }

    @Override
    public void fill(AIContext aiContext, ChatContext context, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var filePath = aiContext.getPath();
        programingLanguage.getFromPath(filePath).ifPresent(lang -> context.programingLanguage = lang);
    }
}
