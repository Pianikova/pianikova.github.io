/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.e1c.edt.ai.assistent.model.CompletionRequest;
import org.e1c.edt.ai.assistent.model.GlobalContext;
import org.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import org.e1c.edt.ai.assistent.model.LocalContext;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

class CompletionRequestFactory
    implements ICompletionRequestFactory, IGlobalContextRequestFactory
{
    private final ILog log;
    private final ITextNormilizer textNormilizer;
    private final IContextEntities contextEntities;
    private final IHashTools hashTools;
    private final IUISettings uiSettings;
    private final Cache<String, GlobalContextUpdate> enitiCache =
        CacheBuilder.newBuilder().weakKeys().maximumSize(1024).expireAfterWrite(15, TimeUnit.MINUTES).build();

    @Inject
    public CompletionRequestFactory(ILog log, ITextNormilizer textNormilizer, IContextEntities contextEntities,
        IHashTools hashTools, IUISettings uiSettings)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(textNormilizer);
        Preconditions.checkNotNull(contextEntities);
        Preconditions.checkNotNull(hashTools);
        Preconditions.checkNotNull(uiSettings);
        this.log = log;
        this.textNormilizer = textNormilizer;
        this.contextEntities = contextEntities;
        this.hashTools = hashTools;
        this.uiSettings = uiSettings;
    }

    @Override
    public CompletionRequest createCompletion(AIContext aiContext, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var sendExtendedContext = uiSettings.sendContext();
        var localContext = new LocalContext();
        localContext.prefix = textNormilizer.normalize(aiContext.getPrefix());
        localContext.suffix = textNormilizer.normalize(aiContext.getSufix());
        localContext.path = aiContext.getPath();
        localContext.offset = aiContext.getSourceOffset();
        var globalContext = new GlobalContext();
        try (var measurement = statistics.measureDuration(StatisticsType.CONTEXT_DURATUION))
        {
            contextEntities.fill(aiContext, localContext, globalContext,
                action -> {
                    return action.getDataType() == DataType.HASH
                        || (sendExtendedContext && (action.getField() == Fields.RELATED_FUNCTIONS
                            || action.getField() == Fields.RELATED_OBJECTS));
                },
                statistics, cancellationToken);
        }
        catch (Exception error)
        {
            log.logError(error);
        }

        var request = new CompletionRequest();
        request.localContext = localContext;
        request.globalContext = globalContext;
        return request;
    }

    @Override
    public List<GlobalContextUpdate> createGlobalContextUpdates(AIContext aiContext,
        HashSet<String> hashes,
        HashSet<String> fields,
        IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var existingUpdates = new HashMap<String, GlobalContextUpdate>();
        for (var hash : hashes)
        {
            var obj = enitiCache.getIfPresent(hash);
            if (obj != null)
            {
                existingUpdates.put(hash, obj);
            }
        }

        var localContext = new LocalContext();
        var globalContext = new GlobalContext();
        try (var measurement = statistics.measureDuration(StatisticsType.CONTEXT_DURATUION))
        {
            contextEntities.fill(
                aiContext, localContext, globalContext, action -> {
                    return action.getDataType() == DataType.HASH || (!existingUpdates.containsKey(action.getHash())
                        && (fields.contains(action.getField()) || hashes.contains(action.getHash())));
                },
                statistics, cancellationToken);
        }
        catch (Exception error)
        {
            log.logError(error);
        }

        var result = new ArrayList<GlobalContextUpdate>();
        var path = aiContext.getPath();
        for (var existingUpdate : existingUpdates.values())
        {
            result.add(existingUpdate);
        }

        if (globalContext.formEntity != null)
        {
            var request = new GlobalContextUpdate();
            request.path = path;
            request.field = Fields.FORM;
            request.hash = globalContext.form;
            request.value = globalContext.formEntity;
            result.add(request);
        }

        if (globalContext.metaEntity != null)
        {
            var request = new GlobalContextUpdate();
            request.path = path;
            request.field = Fields.META;
            request.hash = globalContext.meta;
            request.value = globalContext.metaEntity;
            result.add(request);
        }

        if (globalContext.localFunctions != null && !globalContext.localFunctions.isEmpty())
        {
            var request = new GlobalContextUpdate();
            request.path = path;
            request.field = Fields.LOCAL_FUNCTIONS;
            request.value = globalContext.localFunctions;
            result.add(request);
        }

        if (globalContext.localFunctionsEntities != null && !globalContext.localFunctionsEntities.isEmpty())
        {
            for (var localFunction : globalContext.localFunctionsEntities.values())
            {
                var request = new GlobalContextUpdate();
                request.path = path;
                request.hash = hashTools.format(localFunction.Hash);
                request.field = Fields.LOCAL_FUNCTIONS + '.' + request.hash;
                request.value = localFunction.Value;
                result.add(request);
            }
        }

        return result;
    }
}