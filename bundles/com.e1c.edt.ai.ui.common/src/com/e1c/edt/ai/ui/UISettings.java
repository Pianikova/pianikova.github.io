/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.Optional;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import com.e1c.edt.ai.ISettingsProvider;
import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.IUISettings;
import com.e1c.edt.ai.ParametersParser;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

@SuppressWarnings("restriction")
class UISettings
    implements IUISettings
{
    private final ISettingsStore settingsStore;
    private final ISettingsProvider settingsProvider;

    @Inject
    public UISettings(ISettingsStore settingsStore, ISettingsProvider settingsProvider)
    {
        Preconditions.checkNotNull(settingsStore);
        Preconditions.checkNotNull(settingsProvider);
        this.settingsStore = settingsStore;
        this.settingsProvider = settingsProvider;
    }

    @Override
    public CodeCompletionPolicy getCodeCompletionPolicy()
    {
        var id = settingsStore.getString(ISettingsStore.CODE_COMPLETION_POLICY);
        return CodeCompletionPolicy.parse(id);
    }

    @Override
    public void setCodeCompletionPolicy(CodeCompletionPolicy policy)
    {
        settingsStore.setString(ISettingsStore.CODE_COMPLETION_POLICY, policy.getId());
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
        return Duration.ofMillis(Optional.ofNullable(settingsProvider.getSettings().getLlmParameters().minDelay)
            .orElse(ParametersParser.DEFAULT_MIN_DELAY));
    }

    @Override
    public Duration getTimeout()
    {
        return Duration.ofMillis(Optional.ofNullable(settingsProvider.getSettings().getLlmParameters().timeout)
            .orElse(ParametersParser.DEAULT_TIMEOUT));
    }

    @Override
    public String getLineSeparator()
    {
        return System.lineSeparator();
    }

    @Override
    public boolean sendExtendedContext()
    {
        return Optional.ofNullable(settingsProvider.getSettings().getLlmParameters().extendedContext).orElse(false);
    }

    @Override
    public boolean sendGlobalContext()
    {
        return Optional.ofNullable(settingsProvider.getSettings().getLlmParameters().globalContext).orElse(false);
    }

    @SuppressWarnings("nls")
    @Override
    public String getLanguage()
    {
        return Optional.ofNullable(settingsStore.getString(ISettingsStore.LANGUAGE))
            .orElse(Platform.getNL().startsWith("ru_") ? "Russian" : "English");
    }

    @Override
    public String getTheme()
    {
        var engine = PlatformUI.getWorkbench().getService(IThemeEngine.class);
        if (engine != null) {
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
        return Optional.ofNullable(settingsProvider.getSettings().getLlmParameters().verbosity)
            .orElse(ParametersParser.DEFAULT_VERBOSITY);
    }
}
