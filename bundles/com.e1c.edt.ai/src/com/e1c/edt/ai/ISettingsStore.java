/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Optional;

public interface ISettingsStore
{
    public final static String CLIENT_TOKEN = "stringPreferenceClientID"; //$NON-NLS-1$
    public final static String CODE_COMPLETION_POLICY = "stringPreferenceCodeCompletionPolicy"; //$NON-NLS-1$
    public final static String CODE_COMPLETION_LINES_COUNT = "stringPreferenceCodeCompletionLinesCount"; //$NON-NLS-1$
    public final static String LANGUAGE = "stringPreferenceLanguage"; //$NON-NLS-1$
    public final static String PARAMETERS = "stringPreferenceLLMParameters"; //$NON-NLS-1$

    public final static int MIN_ASSISTANT_TEXT_SIZE = 16;
    public final static int DEFAULT_CODE_COMPLETION_LINES_COUNT = 5;
    public final static int MAX_CODE_COMPLETION_LINES_COUNT = 64;

    Optional<String> getString(String key);

    void setString(String key, String value);

    Optional<Integer> getInt(String key);

    Optional<Boolean> getBoolean(String key);

    <T> Optional<T> getValue(String key, Class<T> classOfT);

    <T> void setValue(String key, T value);
}
