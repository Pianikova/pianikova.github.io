/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import org.e1c.edt.ai.assistent.model.LocalContext;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class LocalContextFactory
    implements ILocalContextFactory
{
    private final ILog log;
    private final ITextNormilizer textNormilizer;
    private final IContextEntities contextEntities;

    @Inject
    public LocalContextFactory(ILog log, ITextNormilizer textNormilizer, IContextEntities contextEntities)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(textNormilizer);
        Preconditions.checkNotNull(contextEntities);
        this.log = log;
        this.textNormilizer = textNormilizer;
        this.contextEntities = contextEntities;
    }

    @Override
    public LocalContext create(AIContext aiContext, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var localContext = new LocalContext();
        localContext.prefix = textNormilizer.normalize(aiContext.getPrefix());
        localContext.suffix = textNormilizer.normalize(aiContext.getSufix());
        localContext.path = aiContext.getPath();
        localContext.offset = aiContext.getSourceOffset();
        try (var measurement = statistics.measureDuration(StatisticsType.CONTEXT_DURATUION))
        {
            contextEntities.fill(aiContext, localContext, statistics, cancellationToken);
        }
        catch (Exception error)
        {
            log.logError(error);
        }

        return localContext;
    }
}
