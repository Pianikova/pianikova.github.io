/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

public interface ISettingsStore
{
    public final static String CLIENT_TOKEN = "stringPreferenceClientID"; //$NON-NLS-1$
    public final static String CLIENT_UID = "stringPreferenceClientUID"; //$NON-NLS-1$
    public final static String APIURL = "stringPreferenceApiURL"; //$NON-NLS-1$
    public final static String LLM_PARAMETERS = "stringPreferenceLLMParameters"; //$NON-NLS-1$
    public final static String CODE_COMPLETION_LINES_COUNT = "stringPreferenceCodeCompletionLinesCount"; //$NON-NLS-1$
    public final static String CONTINUOUS_CODE_COMPLETION = "stringPreferenceContinuousCodeCompletion"; //$NON-NLS-1$
    public final static String CODE_COMPLETION_MIN_REQUST_DELAY = "stringPreferenceCodeCompletionMinRequestDelay"; //$NON-NLS-1$
    public final static String SEND_CONTEXT = "stringPreferenceSendContext"; //$NON-NLS-1$
    public final static String TIMEOUT = "stringTimeoutMs"; //$NON-NLS-1$
    public final static String TRACE_MODE = "stringPreferenceTraceMode"; //$NON-NLS-1$

    public final static int MIN_ASSISTANT_TEXT_SIZE = 16;
    public final static int DEFAULT_CODE_COMPLETION_LINES_COUNT = 5;
    public final static int MAX_CODE_COMPLETION_LINES_COUNT = 64;
    public final static int MAX_TIMEOUT = Integer.MAX_VALUE;

    String getString(String key);

    int getInt(String key);

    boolean getBoolean(String key);

    <T> Optional<T> getValue(String key, Class<T> classOfT);

    <T> void setValue(String key, T value);
}
