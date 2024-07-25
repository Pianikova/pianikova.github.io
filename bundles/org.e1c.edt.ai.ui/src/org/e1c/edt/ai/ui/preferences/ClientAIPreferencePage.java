/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.preferences;

import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.IValidator;
import org.e1c.edt.ai.client.ClientAI;
import org.e1c.edt.ai.ui.AIUIModule;
import org.e1c.edt.ai.ui.Activator;
import org.eclipse.jface.preference.BooleanFieldEditor;
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
    @Named(AIUIModule.URL)
    IValidator<String> urlValidator;
    @Inject
    @Named(AIUIModule.PARAMETERS)
    IValidator<String> parametersValidator;
    @Inject
    IPreferenceStore preferenceStore;

    public ClientAIPreferencePage()
    {
        super(GRID);
        Activator.injectMembers(this);
        setPreferenceStore(preferenceStore);
        setDescription(Messages.ClientAIPreferencePage_Service_parameters);
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
        addField(new ValidatingStringFieldEditor(ISettingsStore.APIURL, Messages.ClientAIPreferencePage_Api_URL,
            parent, urlValidator));

        addField(new ValidatingStringFieldEditor(ISettingsStore.CHATURL, Messages.ClientAIPreferencePage_Chat_URL,
            parent, urlValidator));

        addField(
            new StringFieldEditor(ISettingsStore.CLIENT_TOKEN, Messages.ClientAIPreferencePage_Client_token,
                parent));

        addField(
            new StringFieldEditor(ISettingsStore.DATABASE_NAME, Messages.ClientAIPreferencePage_Database_name,
                parent));

        addField(new StringFieldEditor(ISettingsStore.MODEL_NAME, Messages.ClientAIPreferencePage_AI_model,
            parent));

        addField(
            new StringFieldEditor(ISettingsStore.TAGS, Messages.ClientAIPreferencePage_Tags, parent));

        addField(
            new StringFieldEditor(ISettingsStore.ACCESS_ROLES, Messages.ClientAIPreferencePage_Access_roles,
                parent));

        addField(
            new StringFieldEditor(ISettingsStore.DOCUMENT_PATH, Messages.ClientAIPreferencePage_Document_path,
                parent));

        addField(new ValidatingStringFieldEditor(ISettingsStore.LLM_PARAMETERS,
            Messages.ClientAIPreferencePage_LLL_parameters,
            parent, parametersValidator));

        var codeCompletionLinesCount = new IntegerFieldEditor(ISettingsStore.CODE_COMPLETION_LINES_COUNT,
            Messages.ClientAIPreferencePage_CodeCompletionLinesCount, parent);
        codeCompletionLinesCount.setValidRange(1, ISettingsStore.MAX_CODE_COMPLETION_LINES_COUNT);
        addField(codeCompletionLinesCount);

        addField(new BooleanFieldEditor(ISettingsStore.CONTINUOUS_CODE_COMPLETION,
            Messages.ClientAIPreferencePage_ContinuousCodeCompletition, parent));
    }

    @Override
    public void init(IWorkbench workbench)
    {
        // Empty stub
    }
}
