/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.preferences;

import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.IValidator;
import org.e1c.edt.ai.client.ClientAI;
import org.e1c.edt.ai.ui.Activator;
import org.e1c.edt.ai.ui.Composition;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

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
    private IValidator<String> urlValidator;
    private IValidator<String> parametersValidator;

    public ClientAIPreferencePage()
    {
        super(GRID);
        urlValidator = Composition.getURLValidator();
        parametersValidator = Composition.getParametersValidator();
        setPreferenceStore(Activator.getDefault().getPreferenceStore());
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
        addField(new ValidatingStringFieldEditor(ISettingsStore.APIURL, Messages.ClientAIPreferencePage_Api_URL,
            getFieldEditorParent(), urlValidator));

        addField(new ValidatingStringFieldEditor(ISettingsStore.CHATURL, Messages.ClientAIPreferencePage_Chat_URL,
            getFieldEditorParent(), urlValidator));

        addField(
            new StringFieldEditor(ISettingsStore.CLIENTTOKEN, Messages.ClientAIPreferencePage_Client_token,
                getFieldEditorParent()));

        addField(
            new StringFieldEditor(ISettingsStore.DATABASENAME, Messages.ClientAIPreferencePage_Database_name,
                getFieldEditorParent()));

        addField(new StringFieldEditor(ISettingsStore.MODELNAME, Messages.ClientAIPreferencePage_AI_model,
            getFieldEditorParent()));

        addField(
            new StringFieldEditor(ISettingsStore.TAGS, Messages.ClientAIPreferencePage_Tags, getFieldEditorParent()));

        addField(
            new StringFieldEditor(ISettingsStore.ACCESSROLES, Messages.ClientAIPreferencePage_Access_roles,
                getFieldEditorParent()));

        addField(
            new StringFieldEditor(ISettingsStore.DOCUMENTPATH, Messages.ClientAIPreferencePage_Document_path,
                getFieldEditorParent()));

        addField(new ValidatingStringFieldEditor(ISettingsStore.LLMPARAMETERS,
            Messages.ClientAIPreferencePage_LLL_parameters,
            getFieldEditorParent(), parametersValidator));

        var maxAssistTextSize = new IntegerFieldEditor(ISettingsStore.MAXASSISTANTTEXTSIZE,
            Messages.ClientAIPreferencePage_MaxAssistantTextSize,
            getFieldEditorParent());
        maxAssistTextSize.setValidRange(ISettingsStore.MINASSISTANTTEXTSIZE,
            ISettingsStore.DEFAULTMAXASSISTANTTEXTSIZE);
        addField(maxAssistTextSize);
    }

    @Override
    public void init(IWorkbench workbench)
    {
        // Empty stub
    }
}
