/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.IUISettings;
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
}
