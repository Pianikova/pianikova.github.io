/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IValidator;
import org.e1c.edt.ai.ParametersParser;
import org.e1c.edt.ai.SettingsProvider;
import org.e1c.edt.ai.URLValidator;
import org.e1c.edt.ai.assistent.AICodeAssistant;
import org.e1c.edt.ai.assistent.IAICodeAssistant;
import org.e1c.edt.ai.ui.preferences.PreferenceStoreToSettingsStoreAdapter;

public class Composition
{
    private static final Log Log = new Log();
    private static final PreferenceStoreToSettingsStoreAdapter PreferenceStoreToSettingsStoreAdapter =
        new PreferenceStoreToSettingsStoreAdapter(Activator.getDefault().getPreferenceStore());
    private static final ParametersParser ParametersParser = new ParametersParser();
    private static final SettingsProvider SettingsProvider =
        new SettingsProvider(Log, PreferenceStoreToSettingsStoreAdapter, ParametersParser);
    private static final AICodeAssistant CodeAssistant = new AICodeAssistant(SettingsProvider);
    private static final CodeAssistentText CodeAssistentText =
        new CodeAssistentText(Log, SettingsProvider);
    private static final Chat Chat = new Chat(Log, SettingsProvider);
    private static final URLValidator URLValidator = new URLValidator();


    public static ILog getLog()
    {
        return Log;
    }

    public static IAICodeAssistant getCodeAssistant()
    {
        return CodeAssistant;
    }

    public static IValidator<String> getParametersValidator()
    {
        return ParametersParser;
    }

    public static IValidator<String> getURLValidator()
    {
        return URLValidator;
    }

    public static IChat getChat()
    {
        return Chat;
    }

    public static IChatDialog getChatDialog()
    {
        return Chat;
    }

    public static ICodeAssistentText getCodeAssistentText()
    {
        return CodeAssistentText;
    }
}
