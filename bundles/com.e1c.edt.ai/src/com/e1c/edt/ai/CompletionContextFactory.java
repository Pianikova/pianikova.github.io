/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.TimeUnit;

import com.e1c.edt.ai.assistent.model.GlobalContext;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.LocalContext;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

class CompletionContextFactory
    implements ILocalContextFactory, IGlobalContextFactory, IGlobalContextRequestFactory
{
    private static final String FIELD_PREFIX = Fields.LOCAL_FUNCTIONS + '.';
    private final ILog log;
    private final ITextNormilizer textNormilizer;
    private final IContextEntities contextEntities;
    private final IHashTools hashTools;
    private final IUISettings uiSettings;
    private final ISettingsProvider settingsProvider;
    private final Cache<String, GlobalContextUpdate> enitiCache =
        CacheBuilder.newBuilder().maximumSize(1024).expireAfterWrite(15, TimeUnit.MINUTES).build();

    @Inject
    public CompletionContextFactory(ILog log, ITextNormilizer textNormilizer, IContextEntities contextEntities,
        IHashTools hashTools, IUISettings uiSettings, ISettingsProvider settingsProvider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(textNormilizer);
        Preconditions.checkNotNull(contextEntities);
        Preconditions.checkNotNull(hashTools);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(settingsProvider);
        this.log = log;
        this.textNormilizer = textNormilizer;
        this.contextEntities = contextEntities;
        this.hashTools = hashTools;
        this.uiSettings = uiSettings;
        this.settingsProvider = settingsProvider;
    }

    @Override
    public LocalContext createLocalContext(AIContext aiContext, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var sendExtendedContext = uiSettings.sendExtendedContext();
        var localContext = new LocalContext();
        localContext.prefix = textNormilizer.normalize(aiContext.getPrefix());
        localContext.suffix = textNormilizer.normalize(aiContext.getSufix());
        localContext.path = aiContext.getPath();
        localContext.offset = aiContext.getSourceOffset();
        var globalContext = new GlobalContext();
        try (var measurement = statistics.measureDuration(StatisticsType.LOCAL_CONTEXT_DURATUION))
        {
            contextEntities.fill(aiContext, localContext, globalContext,
                action -> {
                    return sendExtendedContext && (Fields.RELATED_FUNCTIONS.equals(action.getField())
                        || Fields.RELATED_OBJECTS.equals(action.getField()));
                },
                statistics, cancellationToken);
        }
        catch (Exception error)
        {
            log.logError(error);
        }

        return localContext;
    }

    @Override
    public GlobalContext createGlobalContext(AIContext aiContext, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var localContext = new LocalContext();
        var globalContext = new GlobalContext();
        try (var measurement = statistics.measureDuration(StatisticsType.GLOBAL_CONTEXT_DURATUION))
        {
            contextEntities.fill(aiContext, localContext, globalContext,
                action -> action.getDataType() == DataType.HASH || Fields.CONFIGURATION_NAME.equals(action.getField()),
                statistics, cancellationToken);
        }
        catch (Exception error)
        {
            log.logError(error);
        }

        globalContext.formEntity = null;
        globalContext.metaEntity = null;
        globalContext.localFunctionsEntities = null;
        return globalContext;
    }

    @Override
    public List<GlobalContextUpdate> createGlobalContextUpdates(AIContext aiContext,
        GlobalContext globalContext,
        IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var settings = settingsProvider.getSettings();
        var path = aiContext.getPath();
        var result = new ArrayList<GlobalContextUpdate>();

        var request = new GlobalContextUpdate();
        request.field = Fields.CONFIGURATION_NAME;
        var settingsConfigurationName = settings.getLlmParameters().configurationName;
        if (settingsConfigurationName != null && !settingsConfigurationName.isBlank())
        {
            request.value = settingsConfigurationName;
        }
        else
        {
            request.value = globalContext.configurationName != null ? globalContext.configurationName : ""; //$NON-NLS-1$
        }

        result.add(request);

        if (globalContext.form != null || globalContext.formEntity != null)
        {
            request = new GlobalContextUpdate();
            request.path = path;
            request.field = Fields.FORM;
            request.hash = globalContext.form;
            request.value = globalContext.formEntity;
            result.add(request);
        }

        if (globalContext.meta != null || globalContext.metaEntity != null)
        {
            request = new GlobalContextUpdate();
            request.path = path;
            request.field = Fields.META;
            request.hash = globalContext.meta;
            request.value = globalContext.metaEntity;
            result.add(request);
        }

        if (globalContext.localFunctions != null && !globalContext.localFunctions.isEmpty())
        {
            request = new GlobalContextUpdate();
            request.path = path;
            request.field = Fields.LOCAL_FUNCTIONS;
            request.value = globalContext.localFunctions;
            result.add(request);
        }

        if (globalContext.localFunctionsEntities != null && !globalContext.localFunctionsEntities.isEmpty())
        {
            for (var localFunction : globalContext.localFunctionsEntities.entrySet())
            {
                request = new GlobalContextUpdate();
                request.path = path;
                request.hash = hashTools.format(localFunction.getValue().Hash, true);
                request.field = FIELD_PREFIX + localFunction.getKey();
                request.value = localFunction.getValue().Value;
                result.add(request);
            }
        }

        return result;
    }

    @Override
    public List<GlobalContextUpdate> createGlobalContextUpdates(AIContext aiContext, HashSet<String> hashes,
        HashSet<String> fields, IStatistics statistics, ICancellationToken cancellationToken)
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
        try (var measurement = statistics.measureDuration(StatisticsType.GLOBAL_CONTEXT_HASHING_DURATUION))
        {
            contextEntities.fill(aiContext, localContext, globalContext, action -> {
                switch (action.getDataType())
                {
                case HASH:
                    return fields.contains(action.getField());

                case DATA:
                    var hash = action.getHash();
                    return hash == null || !existingUpdates.containsKey(hash) || hashes.contains(hash);

                default:
                    return false;
                }

            }, statistics, cancellationToken);
        }
        catch (Exception error)
        {
            log.logError(error);
        }

        var result = createGlobalContextUpdates(aiContext, globalContext, statistics, cancellationToken);
        for (var existingUpdate : existingUpdates.values())
        {
            result.add(existingUpdate);
        }

        return result;
    }
}