/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.time.Duration;

import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.IUISettings;
import org.eclipse.core.runtime.Platform;
import org.eclipse.e4.ui.css.swt.theme.IThemeEngine;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import com.google.inject.Inject;

@SuppressWarnings("restriction")
class UISettings
    implements IUISettings
{
    private final ISettingsStore settingsStore;

    @Inject
    public UISettings(ISettingsStore settingsStore)
    {
        this.settingsStore = settingsStore;
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
        return Duration.ofMillis(settingsStore.getInt(ISettingsStore.CODE_COMPLETION_MIN_REQUST_DELAY));
    }

    @Override
    public Duration getTimeout()
    {
        return Duration.ofMillis(settingsStore.getInt(ISettingsStore.TIMEOUT));
    }

    @Override
    public String getLineSeparator()
    {
        return System.lineSeparator();
    }

    @Override
    public boolean sendContext()
    {
        return settingsStore.getBoolean(ISettingsStore.SEND_CONTEXT);
    }

    @Override
    public String getLanguage()
    {
        return Platform.getNL().equalsIgnoreCase("ru_RU") ? "Russian" : "English"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
        return settingsStore.getBoolean(ISettingsStore.TRACE_MODE);
    }
}
