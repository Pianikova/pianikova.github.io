/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.CodeCompletionTokenizer;
import org.e1c.edt.ai.ICodeCompletionTokenizer;
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
    private static final Log LOG = new Log();
    private static final Dispatcher DISPATCHER = new Dispatcher(LOG);
    private static final PreferenceStoreToSettingsStoreAdapter PREFERENCE_STORE_TO_SETTINGS_STORE_ADAPTER =
        new PreferenceStoreToSettingsStoreAdapter(Activator.getDefault().getPreferenceStore());
    private static final ParametersParser PARAMETERS_PARSER = new ParametersParser();
    private static final SettingsProvider SETTINGS_PROVIDER =
        new SettingsProvider(LOG, PREFERENCE_STORE_TO_SETTINGS_STORE_ADAPTER, PARAMETERS_PARSER);
    private static final AICodeAssistant CODE_ASSISTANT = new AICodeAssistant(SETTINGS_PROVIDER);
    private static final UI UI = new UI(LOG);
    private static final Chat CHAT = new Chat(LOG, SETTINGS_PROVIDER, UI, DISPATCHER);
    private static final URLValidator URL_VALIDATOR = new URLValidator();
    private static final AIContextImpl AI_CONTEXT_IMPL = new AIContextImpl(LOG, UI, SETTINGS_PROVIDER);
    private static final CodeCompletionTokenizer CODECOMPLETION_TOKENIZER = new CodeCompletionTokenizer();

    public static ILog getLog()
    {
        return LOG;
    }

    public static IDispatcher getDispatcher()
    {
        return DISPATCHER;
    }

    public static IAICodeAssistant getCodeAssistant()
    {
        return CODE_ASSISTANT;
    }

    public static IValidator<String> getParametersValidator()
    {
        return PARAMETERS_PARSER;
    }

    public static IValidator<String> getURLValidator()
    {
        return URL_VALIDATOR;
    }

    public static IChat getChat()
    {
        return CHAT;
    }

    public static IChatDialog getChatDialog()
    {
        return CHAT;
    }

    public static IAIContext getAIContext()
    {
        return AI_CONTEXT_IMPL;
    }

    public static IUI getUI()
    {
        return UI;
    }

    public static ICodeCompletionTokenizer getCodeCompletionTokenizer()
    {
        return CODECOMPLETION_TOKENIZER;
    }
}
