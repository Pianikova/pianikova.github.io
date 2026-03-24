/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

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
        bind(ResponseCache.class).in(Singleton.class);
        bind(IResponseCache.class).to(ResponseCache.class);
        bind(IStateListener.class).to(ResponseCache.class);
        bind(ISessionService.class).to(SessionService.class).in(Singleton.class);
        bind(IFeedbackService.class).to(FeedbackService.class).in(Singleton.class);
        bind(ISettingsTracker.class).to(SettingsTracker.class).in(Singleton.class);
        bind(ITextPreprocessor.class).to(TextPreprocessor.class).in(Singleton.class);
        bind(IThreadManager.class).to(ThreadManager.class).in(Singleton.class);
        bind(IGlobalContextService.class).to(GlobalContextService.class).in(Singleton.class);
        bind(ICompressor.class).to(Compressor.class).in(Singleton.class);
        bind(ITools.class).to(Tools.class).in(Singleton.class);
        bind(ISessionCall.class).to(SessionCall.class).in(Singleton.class);
        bind(IDiagnosticsFactory.class).to(DiagnosticsFactory.class).in(Singleton.class);
        bind(IDiagnosticContext.class).to(DiagnosticContext.class).in(Singleton.class);
        bind(IDiagnosticMapper.class).to(DiagnosticMapper.class).in(Singleton.class);
        bind(ICACertificateReporter.class).to(CACertificateReporter.class).in(Singleton.class);
        bind(ITokenCheck.class).to(TokenCheck.class).in(Singleton.class);
        // @formatter:on
    }
}
