/**
 *
 */
package com.e1c.edt.ai.ui;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;
import java.util.function.Supplier;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import com.e1c.edt.ai.IDefaultSettings;
import com.e1c.edt.ai.IIdProvider;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IParser;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsSetter;
import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.ParametersParser;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.assistent.model.Parameters;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

@SuppressWarnings("restriction")
public class Settings
    implements ISettings, ISettingsSetter
{
    private final ILog log;
    private final ISettingsStore settingsStore;
    private final IParser<String, Parameters> parametersParser;
    private final IIdProvider idProvider;
    private final IDefaultSettings defaultSettings;
    private final Parameters defaultParameters;
    private final Cache<Object, Optional<Parameters>> parametersCache =
        CacheBuilder.newBuilder().maximumSize(128).build();
    private Optional<Parameters> defaultSessionParameters = Optional.empty();

    @Inject
    public Settings(ILog log, ISettingsStore settingsStore, IParser<String, Parameters> parametersParser,
        IIdProvider idProvider,
        IDefaultSettings defaultSettings)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settingsStore);
        Preconditions.checkNotNull(parametersParser);
        Preconditions.checkNotNull(idProvider);
        Preconditions.checkNotNull(defaultSettings);
        this.log = log;
        this.settingsStore = settingsStore;
        this.parametersParser = parametersParser;
        this.idProvider = idProvider;
        this.defaultSettings = defaultSettings;
        defaultParameters = new Parameters(defaultSettings);
    }

    @SuppressWarnings("nls")
    @Override
    public String getClientToken()
    {
        var clientToken = settingsStore.getString(ISettingsStore.CLIENT_TOKEN);
        if (clientToken != null)
        {
            clientToken = clientToken.trim();
        }
        else
        {
            clientToken = "";
        }

        return clientToken;
    }

    @Override
    public String getClientUniqueId()
    {
        return idProvider.getId();
    }

    @Override
    public CodeCompletionPolicy getCodeCompletionPolicy()
    {
        var id = settingsStore.getString(ISettingsStore.CODE_COMPLETION_POLICY);
        return CodeCompletionPolicy.parse(id);
    }

    @Override
    public int getTabWidth()
    {
        return EditorsUI.getPreferenceStore().getInt(AbstractDecoratedTextEditorPreferenceConstants.EDITOR_TAB_WIDTH);
    }

    @Override
    public int getCodeCompletionLinesCount()
    {
        return settingsStore.getInt(ISettingsStore.CODE_COMPLETION_LINES_COUNT);
    }

    @Override
    public Duration getMinRequestDelay()
    {
        return Duration
            .ofMillis(
                getParameterValue(null, parameters -> parameters.minDelay, () -> ParametersParser.DEFAULT_MIN_DELAY));
    }

    @Override
    public Duration getTimeout()
    {
        return Duration
            .ofMillis(getParameterValue(null, parameters -> parameters.timeout, () -> ParametersParser.DEAULT_TIMEOUT));
    }

    @Override
    public String getLineSeparator()
    {
        return System.lineSeparator();
    }

    @Override
    public int getPrefixLength(ProjectId projectId)
    {
        return getParameterValue(projectId, parameters -> parameters.prefixLength,
            () -> ParametersParser.DEFAULT_PREFIX_LEN);
    }

    @Override
    public int getSuffixLength(ProjectId projectId)
    {
        return getParameterValue(projectId, parameters -> parameters.suffixLength,
            () -> ParametersParser.DEFAULT_SUFFIX_LEN);
    }

    @Override
    public boolean sendExtendedContext()
    {
        return getParameterValue(null, parameters -> parameters.extendedContext, () -> false);
    }

    @Override
    public boolean sendGlobalContext(ProjectId projectId)
    {
        return getParameterValue(projectId, parameters -> parameters.globalContext, () -> false);
    }

    @SuppressWarnings("nls")
    @Override
    public String getLanguage()
    {
        return Optional.ofNullable(settingsStore.getString(ISettingsStore.LANGUAGE))
            .map(i -> i.isBlank() ? null : i)
            .orElse(Platform.getNL().startsWith("ru_") ? "Russian" : "English");
    }

    @Override
    public String getTheme()
    {
        var engine = PlatformUI.getWorkbench().getService(IThemeEngine.class);
        if (engine != null)
        {
            var activeTheme = engine.getActiveTheme();
            if (activeTheme != null)
            {
                if (activeTheme.getId().toLowerCase().contains("dark")) //$NON-NLS-1$
                {
                    return "Dark"; //$NON-NLS-1$
                }
            }
        }

        return "Default"; //$NON-NLS-1$
    }

    @Override
    public Verbosity getVerbosity()
    {
        return getParameterValue(null, parameters -> Optional.ofNullable(parameters.verbosity),
            () -> ParametersParser.DEFAULT_VERBOSITY);
    }

    @Override
    public Optional<String> getResources()
    {
        var resource = getParameterValue(null, parameters -> parameters.resources, () -> null);
        return (resource == null || resource.isBlank()) ? Optional.empty() : Optional.of(resource);
    }

    @Override
    public int getGitDiffContextLines(ProjectId projectId)
    {
        return getParameterValue(projectId, parameters -> parameters.gitDiffContextLines,
            () -> ParametersParser.DEFAULT_GIT_CONTEXT_LINES);
    }

    @Override
    public URL getUrl()
    {
        return getUserParameters().url;
    }

    @Override
    public URL getChatUrl()
    {
        return getParameterValue(null, parameters -> parameters.chatUrl, () -> {
            try
            {
                return new URL(defaultSettings.getChatUrl());
            }
            catch (MalformedURLException e)
            {
                return null;
            }
        });
    }

    @Override
    public String getHomePage()
    {
        return defaultSettings.getHomePage();
    }

    @Override
    public String getUpdateUrl()
    {
        return getParameterValue(null, parameters -> parameters.updateUrl, () -> defaultSettings.getUpdateUrl());
    }

    @Override
    public String getPluginFeature()
    {
        return defaultSettings.getPluginFeature();
    }

    @Override
    public Parameters getUserParameters()
    {
        return getOptionalUserParameters().orElse(defaultParameters);
    }

    @Override
    public synchronized void applySessionParameters(ProjectId projectId, Parameters sessionParameters)
    {
        parametersCache.put(projectId, Optional.ofNullable(sessionParameters));
        if (defaultSessionParameters.isEmpty())
        {
            defaultSessionParameters = Optional.ofNullable(sessionParameters);
        }
    }

    @Override
    public void setCodeCompletionPolicy(CodeCompletionPolicy codeCompletionPolicy)
    {
        settingsStore.setString(ISettingsStore.CODE_COMPLETION_POLICY, codeCompletionPolicy.getId());
    }

    private synchronized Optional<Parameters> getOptionalUserParameters()
    {
        var parametersStr = settingsStore.getString(ISettingsStore.PARAMETERS);
        if (parametersStr == null || parametersStr.isBlank())
        {
            return Optional.empty();
        }

        try
        {
            return parametersCache.get(parametersStr, () -> parametersParser.parse(parametersStr));
        }
        catch (ExecutionException error)
        {
            log.logError(error);
        }

        return Optional.empty();
    }

    private synchronized Optional<Parameters> getOptionalSessionParameters(ProjectId projectId)
    {
        if (projectId == null)
        {
            return defaultSessionParameters;
        }

        var projectParameters = parametersCache.getIfPresent(projectId);
        if (projectParameters != null && projectParameters.isPresent())
        {
            return projectParameters;
        }

        return defaultSessionParameters;
    }

    private <T> T getParameterValue(ProjectId projectId, Function<Parameters, Optional<T>> valueSelector,
        Supplier<T> defaultValueProvider)
    {
        return getOptionalUserParameters().flatMap(i -> Optional.ofNullable(valueSelector.apply(i)))
            .flatMap(i -> i)
            .orElseGet(
                () -> getOptionalSessionParameters(projectId).flatMap(i -> Optional.ofNullable(valueSelector.apply(i)))
                    .flatMap(i -> i)
                    .orElseGet(() -> defaultValueProvider.get()));
    }
}
