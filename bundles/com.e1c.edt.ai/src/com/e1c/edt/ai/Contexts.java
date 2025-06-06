/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import com.e1c.edt.ai.assistent.model.GlobalContext;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.LocalContext;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class Contexts
    implements ILocalContext, IGlobalContext
{
    private static final String FIELD_PREFIX = Fields.LOCAL_FUNCTIONS + '.';
    private final ILog log;
    private final ITextNormilizer textNormilizer;
    private final IContextEntities contextEntities;
    private final IHashTools hashTools;
    private final IUISettings uiSettings;
    private final ISettingsProvider settingsProvider;

    @Inject
    public Contexts(ILog log, ITextNormilizer textNormilizer, IContextEntities contextEntities,
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
    public LocalContext create(AIContext aiContext, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var sendExtendedContext = uiSettings.sendExtendedContext();
        var localContext = new LocalContext();
        localContext.prefix = textNormilizer.normalize(aiContext.getPrefix());
        localContext.suffix = textNormilizer.normalize(aiContext.getSufix());
        localContext.path = aiContext.getPath();
        localContext.offset = aiContext.getSourceOffset();
        var globalContext = new GlobalContext();
        globalContext.modulePath = aiContext.getPath();
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
    public List<GlobalContextUpdate> getUpdates(AIContext aiContext, boolean sendInitialState,
        IStatistics statistics, ICancellationToken cancellationToken)
    {
        var globalContext = createGlobalContext(aiContext, statistics, cancellationToken);
        return getUpdates(globalContext, sendInitialState, statistics, cancellationToken);
    }

    private GlobalContext createGlobalContext(AIContext aiContext, IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var localContext = new LocalContext();
        var globalContext = new GlobalContext();
        globalContext.modulePath = aiContext.getPath();
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

    private List<GlobalContextUpdate> getUpdates(
        GlobalContext globalContext,
        boolean sendInitialState,
        IStatistics statistics,
        ICancellationToken cancellationToken)
    {
        var result = new ArrayList<GlobalContextUpdate>();
        GlobalContextUpdate request;
        if (sendInitialState && globalContext.configurationName != null)
        {
            request = new GlobalContextUpdate();
            request.field = Fields.CONFIGURATION_NAME;
            var settings = settingsProvider.getSettings();
            var settingsConfigurationName = settings.getLlmParameters().configurationName;
            if (settingsConfigurationName != null && !settingsConfigurationName.isBlank())
            {
                request.value = settingsConfigurationName;
            }
            else
            {
                request.value = globalContext.configurationName;
            }

            result.add(request);
        }

        if (globalContext.formPath != null && globalContext.formHash != null)
        {
            request = new GlobalContextUpdate();
            request.path = globalContext.formPath;
            request.field = Fields.FORM;
            request.hash = globalContext.formHash;
            request.value = globalContext.formEntity;
            result.add(request);
        }

        if (globalContext.metaPath != null && globalContext.metaHash != null)
        {
            request = new GlobalContextUpdate();
            request.path = globalContext.metaPath;
            request.field = Fields.META;
            request.hash = globalContext.metaHash;
            request.value = globalContext.metaEntity;
            result.add(request);
        }

        if (globalContext.modulePath != null && globalContext.modulePath.toLowerCase().endsWith(".bsl")) //$NON-NLS-1$
        {
            if (globalContext.moduleHash != null)
            {
                request = new GlobalContextUpdate();
                request.path = globalContext.modulePath;
                request.field = Fields.LOCAL_FUNCTIONS;
                request.hash = globalContext.moduleHash;
                request.value = globalContext.localFunctions;
                result.add(request);
            }

            if (globalContext.localFunctionsEntities != null && !globalContext.localFunctionsEntities.isEmpty())
            {
                for (var localFunction : globalContext.localFunctionsEntities.entrySet())
                {
                    var val = localFunction.getValue();
                    request = new GlobalContextUpdate();
                    request.path = globalContext.modulePath;
                    request.hash = val.Hash;
                    request.field = FIELD_PREFIX + localFunction.getKey();
                    request.value = val.Value;
                    result.add(request);
                }
            }
        }

        return result;
    }

    @Override
    public List<GlobalContextUpdate> getUpdates(AIContext aiContext, String fileHash,
        HashSet<String> hashes,
        HashSet<String> fields, IStatistics statistics, ICancellationToken cancellationToken)
    {
        var localContext = new LocalContext();
        var globalContext = new GlobalContext();
        globalContext.moduleHash = fileHash;
        globalContext.modulePath = aiContext.getPath();
        try (var measurement = statistics.measureDuration(StatisticsType.GLOBAL_CONTEXT_HASHING_DURATUION))
        {
            contextEntities.fill(aiContext, localContext, globalContext, action -> {
                switch (action.getDataType())
                {
                case HASH:
                    return fields.contains(action.getField());

                case DATA:
                    var hash = action.getHash();
                    return hash == null || fields.contains(Fields.LOCAL_FUNCTIONS) || hashes.contains(hash);

                default:
                    return false;
                }

            }, statistics, cancellationToken);
        }
        catch (Exception error)
        {
            log.logError(error);
        }

        return getUpdates(globalContext, false, statistics, cancellationToken);
    }
}