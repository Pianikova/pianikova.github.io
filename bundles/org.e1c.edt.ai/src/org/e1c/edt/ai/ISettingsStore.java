/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface ISettingsStore
{
    public final static String MODELNAME = "stringPreferenceModelName"; //$NON-NLS-1$
    public final static String CLIENTTOKEN = "stringPreferenceClientID"; //$NON-NLS-1$
    public final static String DATABASENAME = "stringPreferenceDataBaseName"; //$NON-NLS-1$
    public final static String APIURL = "stringPreferenceApiURL"; //$NON-NLS-1$
    public final static String CHATURL = "stringPreferenceChatURL"; //$NON-NLS-1$
    public final static String TAGS = "stringPreferenceTags"; //$NON-NLS-1$
    public final static String ACCESSROLES = "stringPreferenceAccessRoles"; //$NON-NLS-1$
    public final static String DOCUMENTPATH = "stringPreferenceDocumentPath"; //$NON-NLS-1$
    public final static String LLMPARAMETERS = "stringPreferenceLLMParameters"; //$NON-NLS-1$
    public final static String MAXASSISTANTTEXTSIZE = "stringPreferenceMaxAssistantTextSize"; //$NON-NLS-1$

    public final static int DEFAULTMAXASSISTANTTEXTSIZE = 1500;
    public final static int MINASSISTANTTEXTSIZE = 16;

    String getString(String key);

    int getInt(String key);
}
