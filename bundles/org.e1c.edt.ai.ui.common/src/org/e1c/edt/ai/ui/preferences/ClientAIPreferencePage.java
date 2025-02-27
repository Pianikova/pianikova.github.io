/*
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui.preferences;

import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.IValidator;
import org.e1c.edt.ai.ui.AIUICommonModule;
import org.e1c.edt.ai.ui.BaseActivator;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * This class contains fields on the preferences page with AI chat settings.
 * Parameters set on this page could be used when creating a new chat.
 * @see ClientAI
 * @author Bogdan Sushkov
 *
 */
public class ClientAIPreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage
{
    @Inject
    @Named(AIUICommonModule.URL)
    IValidator<String> urlValidator;
    @Inject
    @Named(AIUICommonModule.PARAMETERS)
    IValidator<String> parametersValidator;
    @Inject
    IPreferenceStore preferenceStore;

    public ClientAIPreferencePage()
    {
        super(GRID);
        BaseActivator.injectMembers(this);
        setPreferenceStore(preferenceStore);
    }

    /**
     * Creates the field editors. Field editors are abstractions of
     * the common GUI blocks needed to manipulate various types
     * of preferences. Each field editor knows how to save and
     * restore itself.
     */
    @Override
    public void createFieldEditors()
    {
        var parent = getFieldEditorParent();

        addField(
            new StringFieldEditor(ISettingsStore.CLIENT_TOKEN, Messages.ClientAIPreferencePage_Client_Token, parent));

        addField(
            new BooleanFieldEditor(ISettingsStore.CODE_COMPLETION,
                Messages.ClientAIPreferencePage_CodeCompletitionEnabled,
                parent));

        addField(new BooleanFieldEditor(ISettingsStore.CONTINUOUS_CODE_COMPLETION,
            Messages.ClientAIPreferencePage_ContinuousCodeCompletition, parent));

        var codeCompletionLinesCount = new IntegerFieldEditor(ISettingsStore.CODE_COMPLETION_LINES_COUNT,
            Messages.ClientAIPreferencePage_CodeCompletionLinesCount, parent);
        codeCompletionLinesCount.setValidRange(1, ISettingsStore.MAX_CODE_COMPLETION_LINES_COUNT);
        addField(codeCompletionLinesCount);

        @SuppressWarnings("nls")
        String[][] languages = {
            { Messages.ClientAIPreferencePage_Language_Default, "" },
            { Messages.ClientAIPreferencePage_Language_English, "english" },
            { Messages.ClientAIPreferencePage_Language_Russian, "russian" } };

        addField(
            new ComboFieldEditor(ISettingsStore.LANGUAGE, Messages.ClientAIPreferencePage_Language, languages, parent));

        addField(new ValidatingStringFieldEditor(ISettingsStore.PARAMETERS,
            Messages.ClientAIPreferencePage_Parameters, parent, parametersValidator));
    }

    @Override
    public void init(IWorkbench workbench)
    {
        // Empty stub
    }
}
