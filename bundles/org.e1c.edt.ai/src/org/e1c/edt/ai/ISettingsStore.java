/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

public interface ISettingsStore
{
    public final static String MODEL_NAME = "stringPreferenceModelName"; //$NON-NLS-1$
    public final static String CLIENT_TOKEN = "stringPreferenceClientID"; //$NON-NLS-1$
    public final static String CLIENT_UID = "stringPreferenceClientUID"; //$NON-NLS-1$
    public final static String DATABASE_NAME = "stringPreferenceDataBaseName"; //$NON-NLS-1$
    public final static String APIURL = "stringPreferenceApiURL"; //$NON-NLS-1$
    public final static String CHATURL = "stringPreferenceChatURL"; //$NON-NLS-1$
    public final static String TAGS = "stringPreferenceTags"; //$NON-NLS-1$
    public final static String ACCESS_ROLES = "stringPreferenceAccessRoles"; //$NON-NLS-1$
    public final static String DOCUMENT_PATH = "stringPreferenceDocumentPath"; //$NON-NLS-1$
    public final static String LLM_PARAMETERS = "stringPreferenceLLMParameters"; //$NON-NLS-1$
    public final static String CODE_COMPLETION_LINES_COUNT = "stringPreferenceCodeCompletionLinesCount"; //$NON-NLS-1$
    public final static String CONTINUOUS_CODE_COMPLETION = "stringPreferenceContinuousCodeCompletion"; //$NON-NLS-1$
    public final static String CODE_COMPLETION_MIN_REQUST_DELAY = "stringPreferenceCodeCompletionMinRequestDelay"; //$NON-NLS-1$

    public final static int MIN_ASSISTANT_TEXT_SIZE = 16;
    public final static int DEFAULT_CODE_COMPLETION_LINES_COUNT = 5;
    public final static int MAX_CODE_COMPLETION_LINES_COUNT = 64;

    String getString(String key);

    int getInt(String key);

    boolean getBoolean(String key);
}
