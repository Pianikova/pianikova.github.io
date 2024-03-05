/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.preferences;

import org.e1c.edt.ai.client.ClientAI;
import org.e1c.edt.ai.ui.Activator;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
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
    public final static String MODELNAME = "stringPreferenceModelName"; //$NON-NLS-1$
    public final static String CLIENTTOKEN = "stringPreferenceClientID"; //$NON-NLS-1$
    public final static String DATABASENAME = "stringPreferenceDataBaseName"; //$NON-NLS-1$
    public final static String SERVICEURL = "stringPreferenceServiceURL"; //$NON-NLS-1$
    public final static String TAGS = "stringPreferenceTags"; //$NON-NLS-1$
    public final static String ACCESSROLES = "stringPreferenceAccessRoles"; //$NON-NLS-1$
    public final static String DOCUMENTPATH = "stringPreferenceDocumentPath"; //$NON-NLS-1$

    public ClientAIPreferencePage()
    {
        super(GRID);
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
        addField(
            new StringFieldEditor(SERVICEURL, Messages.ClientAIPreferencePage_Service_URL, getFieldEditorParent()));

        addField(
            new StringFieldEditor(CLIENTTOKEN, Messages.ClientAIPreferencePage_Client_token, getFieldEditorParent()));

        addField(
            new StringFieldEditor(DATABASENAME, Messages.ClientAIPreferencePage_Database_name, getFieldEditorParent()));

        addField(new StringFieldEditor(MODELNAME, Messages.ClientAIPreferencePage_AI_model, getFieldEditorParent()));

        addField(new StringFieldEditor(TAGS, Messages.ClientAIPreferencePage_Tags, getFieldEditorParent()));

        addField(
            new StringFieldEditor(ACCESSROLES, Messages.ClientAIPreferencePage_Access_roles, getFieldEditorParent()));

        addField(
            new StringFieldEditor(DOCUMENTPATH, Messages.ClientAIPreferencePage_Document_path, getFieldEditorParent()));
    }

    @Override
    public void init(IWorkbench workbench)
    {
        // Empty stub
    }
}
