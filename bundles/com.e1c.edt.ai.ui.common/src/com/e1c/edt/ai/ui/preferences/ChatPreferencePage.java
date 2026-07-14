/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;

import com.e1c.edt.ai.ISettingsStore;

/**
 * Страница настроек чата: язык ответов и автооткрытие изменений во вкладке сравнения.
 *
 * @author Nikolay Pianikov
 */
public class ChatPreferencePage
    extends BaseAIPreferencePage
{
    @SuppressWarnings("nls")
    private static final String[][] LANGUAGES = {
        { Messages.ClientAIPreferencePage_Language_Default, "" },
        { Messages.ClientAIPreferencePage_Language_English, "english" },
        { Messages.ClientAIPreferencePage_Language_Russian, "russian" } };

    @Override
    public void createFieldEditors()
    {
        var parent = getFieldEditorParent();

        var comboField = new ComboFieldEditor(ISettingsStore.LANGUAGE, Messages.ClientAIPreferencePage_Language,
            LANGUAGES, parent);
        setLabelTooltip(comboField, parent, Messages.ClientAIPreferencePage_Language_Tooltip);
        addField(comboField);

        var autoOpenDiffPreviewField = new BooleanFieldEditor(ISettingsStore.AUTO_OPEN_DIFF_PREVIEW,
            Messages.ClientAIPreferencePage_AutoOpenDiffPreview, parent);
        addField(autoOpenDiffPreviewField);
    }
}
