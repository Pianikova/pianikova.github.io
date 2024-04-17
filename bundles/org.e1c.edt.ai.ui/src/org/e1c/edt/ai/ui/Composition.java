/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.IValidator;
import org.e1c.edt.ai.ParametersParser;
import org.e1c.edt.ai.assistent.AICodeAssistant;
import org.e1c.edt.ai.assistent.IAICodeAssistant;
import org.e1c.edt.ai.ui.preferences.SettingsProvider;

public class Composition
{
    private static final ParametersParser ParametersParser = new ParametersParser();
    private static final SettingsProvider SettingsProvider =
        new SettingsProvider(Activator.getDefault().getPreferenceStore(), ParametersParser);

    private static final AICodeAssistant CodeAssistant = new AICodeAssistant(SettingsProvider);
    private static final Chat Chat = new Chat(SettingsProvider);

    public static ISettingsProvider getSettingsProvider()
    {
        return SettingsProvider;
    }

    public static IAICodeAssistant getCodeAssistant()
    {
        return CodeAssistant;
    }

    public static IValidator<String> getParametersValidator()
    {
        return ParametersParser;
    }

    public static IChat getChat()
    {
        return Chat;
    }

    public static IChatDialog getChatDialog()
    {
        return Chat;
    }
}
