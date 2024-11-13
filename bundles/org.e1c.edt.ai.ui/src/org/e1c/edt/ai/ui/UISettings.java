/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.time.Duration;

import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.IUISettings;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;

import com.google.inject.Inject;

public class UISettings
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
        return Platform.getNL();
    }
}
