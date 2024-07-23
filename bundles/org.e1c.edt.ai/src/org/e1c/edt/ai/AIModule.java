/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import org.e1c.edt.ai.assistent.CodeAssistant;
import org.e1c.edt.ai.assistent.FeedbackService;
import org.e1c.edt.ai.assistent.HttpClientBuilder;
import org.e1c.edt.ai.assistent.HttpLog;
import org.e1c.edt.ai.assistent.ICodeAssistant;
import org.e1c.edt.ai.assistent.IFeedbackService;
import org.e1c.edt.ai.assistent.IHttpClientBuilder;
import org.e1c.edt.ai.assistent.IHttpLog;
import org.e1c.edt.ai.assistent.IParametersService;
import org.e1c.edt.ai.assistent.IRequestBuilder;
import org.e1c.edt.ai.assistent.IResponseCache;
import org.e1c.edt.ai.assistent.IResponseLineProcessor;
import org.e1c.edt.ai.assistent.IResponseStreamProcessor;
import org.e1c.edt.ai.assistent.ISessionService;
import org.e1c.edt.ai.assistent.ParametersService;
import org.e1c.edt.ai.assistent.RequestBuilder;
import org.e1c.edt.ai.assistent.ResponseCache;
import org.e1c.edt.ai.assistent.ResponseLineProcessor;
import org.e1c.edt.ai.assistent.ResponseStreamProcessor;
import org.e1c.edt.ai.assistent.SessionService;
import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.assistent.model.Session;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;

public class AIModule
    extends AbstractModule
{
    public static final String PARAMETERS = "Parameters"; //$NON-NLS-1$
    public static final String URL = "URL"; //$NON-NLS-1$

    @Override
    protected void configure()
    {
        // @formatter:off
        bind(ParametersParser.class).in(Singleton.class);
        bind(new TypeLiteral<IValidator<String>>() { /**/ }).annotatedWith(Names.named(PARAMETERS)).to(ParametersParser.class);
        bind(new TypeLiteral<IParser<String, Parameters>>(){ /**/ }).to(ParametersParser.class);
        bind(ISettingsProvider.class).to(SettingsProvider.class).in(Singleton.class);
        bind(ICodeCompletionTokenizer.class).to(CodeCompletionTokenizer.class).in(Singleton.class);
        bind(IJson.class).to(Json.class).in(Singleton.class);
        bind(IResponseStreamProcessor.class).to(ResponseStreamProcessor.class).in(Singleton.class);
        bind(IResponseLineProcessor.class).to(ResponseLineProcessor.class).in(Singleton.class);
        bind(ICodeAssistant.class).to(CodeAssistant.class).in(Singleton.class);
        bind(new TypeLiteral<IValidator<String>>() { /**/ }).annotatedWith(Names.named(URL)).to(URLValidator.class).in(Singleton.class);
        bind(IContextSplitter.class).to(ContextSplitter.class).in(Singleton.class);
        bind(IHintTextBuilder.class).to(HintTextBuilder.class).in(Singleton.class);
        bind(IClock.class).to(Clock.class).in(Singleton.class);
        bind(IMath.class).to(Math.class).in(Singleton.class);
        bind(IInputDelayStatistics.class).to(InputDelayStatistics.class).in(Singleton.class);
        bind(IContextInitializer.class).to(ContextInitializer.class).in(Singleton.class);
        bind(IStringNormalizer.class).to(StringNormalizer.class).in(Singleton.class);
        bind(IHistoricalHint.class).to(Hint.class);
        bind(IHintHistory.class).to(HintHistory.class);
        bind(IHintHistory.class).to(HintHistory.class);
        bind(ICodeCompletionContext.class).to(CodeCompletionStatistics.class).in(Singleton.class);

        bind(IHttpLog.class).to(HttpLog.class).in(Singleton.class);
        bind(IHttpClientBuilder.class).to(HttpClientBuilder.class).in(Singleton.class);
        bind(IRequestBuilder.class).to(RequestBuilder.class).in(Singleton.class);
        bind(IParametersService.class).to(ParametersService.class).in(Singleton.class);
        bind(new TypeLiteral<IResponseCache<Parameters>>() { /**/ }).to(new TypeLiteral<ResponseCache<Parameters>>() { /**/ });
        bind(ISessionService.class).to(SessionService.class).in(Singleton.class);
        bind(new TypeLiteral<IResponseCache<Session>>() { /**/ }).to(new TypeLiteral<ResponseCache<Session>>() { /**/ });
        bind(IFeedbackService.class).to(FeedbackService.class).in(Singleton.class);
        // @formatter:on
    }
}
