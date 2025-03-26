/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;

import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import com.e1c.edt.ai.ISettingsProvider;
import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.IUISettings;
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
    public boolean isCodeCompletion()
    {
        return settingsStore.getBoolean(ISettingsStore.CODE_COMPLETION);
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
    public boolean isContinuousCodeCompletion()
    {
        return settingsStore.getBoolean(ISettingsStore.CONTINUOUS_CODE_COMPLETION);
    }

    @Override
    public Duration getMinRequestDelay()
    {
        return Duration.ofMillis(settingsProvider.getSettings().getLlmParameters().minDelay);
    }

    @Override
    public Duration getTimeout()
    {
        return Duration.ofMillis(settingsProvider.getSettings().getLlmParameters().timeout);
    }

    @Override
    public String getLineSeparator()
    {
        return System.lineSeparator();
    }

    @Override
    public boolean sendContext()
    {
        return settingsProvider.getSettings().getLlmParameters().extendedСontext;
    }

    @Override
    public boolean sendGlobalContext()
    {
        return settingsProvider.getSettings().getLlmParameters().globalСontext;
    }

    @Override
    public String getLanguage()
    {
        var language = settingsStore.getString(ISettingsStore.LANGUAGE);
        if (language != null && !language.isBlank())
        {
            return language;
        }

        return Platform.getNL().startsWith("ru_") ? "Russian" : "English"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
    public boolean traceMode()
    {
        return settingsProvider.getSettings().getLlmParameters().trace;
    }

    @Override
    public Verbosity getVerbosiry()
    {
        return settingsProvider.getSettings().getLlmParameters().verbosity;
    }
}
