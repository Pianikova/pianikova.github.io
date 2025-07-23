/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import com.e1c.edt.ai.assistent.model.Parameters;
import com.e1c.edt.ai.assistent.model.Session;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class AssistentModule
    extends AbstractModule
{
    @Override
    protected void configure()
    {
        // @formatter:off
        bind(IResponseStreamProcessor.class).to(ResponseStreamProcessor.class).in(Singleton.class);
        bind(IResponseLineProcessor.class).to(ResponseLineProcessor.class).in(Singleton.class);
        bind(ICodeAssistant.class).to(CodeAssistant.class).in(Singleton.class);
        bind(IHttpLog.class).to(HttpLog.class).in(Singleton.class);
        bind(IHttpClientBuilder.class).to(HttpClientBuilder.class).in(Singleton.class);
        bind(IRequestBuilder.class).to(RequestBuilder.class).in(Singleton.class);
        bind(IParametersService.class).to(ParametersService.class).in(Singleton.class);
        bind(IHealthCheckService.class).to(HealthCheckService.class).in(Singleton.class);
        bind(new TypeLiteral<IResponseCache<Parameters>>() { /**/ }).to(new TypeLiteral<ResponseCache<Parameters>>() { /**/ });
        bind(ISessionService.class).to(SessionService.class).in(Singleton.class);
        bind(new TypeLiteral<IResponseCache<Session>>() { /**/ }).to(new TypeLiteral<ResponseCache<Session>>() { /**/ });
        bind(IFeedbackService.class).to(FeedbackService.class).in(Singleton.class);
        bind(ISettingsTracker.class).to(SettingsTracker.class).in(Singleton.class);
        bind(ITextPreprocessor.class).to(TextPreprocessor.class).in(Singleton.class);
        bind(IStateService.class).to(StateService.class).in(Singleton.class);
        bind(IThreadManager.class).to(ThreadManager.class).in(Singleton.class);
        bind(IGlobalContextService.class).to(GlobalContextService.class).in(Singleton.class);
        bind(ICompressor.class).to(Compressor.class).in(Singleton.class);
        bind(IConversations.class).to(Conversations.class).in(Singleton.class);
        // @formatter:on
    }
}
