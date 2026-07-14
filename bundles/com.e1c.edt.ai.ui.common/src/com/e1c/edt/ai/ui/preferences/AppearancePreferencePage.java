/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.preferences;

import org.eclipse.jface.preference.BooleanFieldEditor;

import com.e1c.edt.ai.ISettingsStore;

/**
 * Страница настроек внешнего вида (Appearance): строка состояния,
 * уведомление об активации.
 *
 * @author Nikolay Pianikov
 */
public class AppearancePreferencePage
    extends BaseAIPreferencePage
{
    @Override
    public void createFieldEditors()
    {
        var parent = getFieldEditorParent();

        var showStatusBarField = new BooleanFieldEditor(ISettingsStore.SHOW_STATUS_BAR,
            Messages.ClientAIPreferencePage_ShowStatusBar, parent);
        addField(showStatusBarField);

        var showActivationInfoField = new BooleanFieldEditor(ISettingsStore.SHOW_ACTIVATION_INFO,
            Messages.ClientAIPreferencePage_ShowActivationInfo, parent);
        addField(showActivationInfoField);
    }
}
