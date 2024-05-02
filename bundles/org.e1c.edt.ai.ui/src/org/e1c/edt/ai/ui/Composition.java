/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.function.Supplier;

import org.e1c.edt.ai.AIContextSplitter;
import org.e1c.edt.ai.CodeCompletionTokenizer;
import org.e1c.edt.ai.IAIContextSplitter;
import org.e1c.edt.ai.ICodeCompletionTokenizer;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IValidator;
import org.e1c.edt.ai.Json;
import org.e1c.edt.ai.ParametersParser;
import org.e1c.edt.ai.SettingsProvider;
import org.e1c.edt.ai.URLValidator;
import org.e1c.edt.ai.assistent.AICodeAssistant;
import org.e1c.edt.ai.assistent.IAICodeAssistant;
import org.e1c.edt.ai.assistent.IResponseStreamContext;
import org.e1c.edt.ai.assistent.LinesCounter;
import org.e1c.edt.ai.assistent.ResponseLineProcessor;
import org.e1c.edt.ai.assistent.ResponseStreamContext;
import org.e1c.edt.ai.assistent.ResponseStreamProcessor;
import org.e1c.edt.ai.ui.preferences.PreferenceStoreToSettingsStoreAdapter;

public class Composition
{
    private static final Log LOG = new Log();
    private static final Dispatcher DISPATCHER = new Dispatcher(LOG);
    private static final PreferenceStoreToSettingsStoreAdapter PREFERENCE_STORE_TO_SETTINGS_STORE_ADAPTER =
        new PreferenceStoreToSettingsStoreAdapter(LOG, Activator.getDefault().getPreferenceStore());
    private static final ParametersParser PARAMETERS_PARSER = new ParametersParser();
    private static final SettingsProvider SETTINGS_PROVIDER =
        new SettingsProvider(LOG, PREFERENCE_STORE_TO_SETTINGS_STORE_ADAPTER, PARAMETERS_PARSER);
    private static final CodeCompletionTokenizer CODECOMPLETION_TOKENIZER = new CodeCompletionTokenizer();
    private static final Supplier<IResponseStreamContext> RESPONSE_STREAM_CONTEXT_FACTORY = new Supplier<>()
    {

        @Override
        public IResponseStreamContext get()
        {
            return new ResponseStreamContext(SETTINGS_PROVIDER, new LinesCounter());
        }
    };

    private static final Json JSON = new Json();
    private static final ResponseLineProcessor RESPONSE_LINE_PROCESSOR = new ResponseLineProcessor(JSON);
    private static final ResponseStreamProcessor RESPONSE_STREAM_PROCESSOR =
        new ResponseStreamProcessor(RESPONSE_STREAM_CONTEXT_FACTORY, RESPONSE_LINE_PROCESSOR);
    private static final AICodeAssistant CODE_ASSISTANT =
        new AICodeAssistant(SETTINGS_PROVIDER, JSON, RESPONSE_STREAM_PROCESSOR);
    private static final UI UI = new UI(LOG);
    private static final IdeApiHandler IDE_API_HANDLER = new IdeApiHandler(LOG, UI);
    private static final Chat CHAT = new Chat(LOG, SETTINGS_PROVIDER, UI, DISPATCHER, IDE_API_HANDLER);
    private static final URLValidator URL_VALIDATOR = new URLValidator();
    private static final IAIContextSplitter AI_CONTEXT_SPLITTER = new AIContextSplitter();
    private static final AIContextProvider AI_CONTEXT_PROVIDER =
        new AIContextProvider(UI, SETTINGS_PROVIDER, AI_CONTEXT_SPLITTER);

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

    public static IAIContextProvider getAIContextProvider()
    {
        return AI_CONTEXT_PROVIDER;
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
