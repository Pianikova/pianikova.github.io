/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import com.e1c.edt.ai.assistent.AssistentModule;
import com.e1c.edt.ai.assistent.model.Parameters;
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
        install(new AssistentModule());
        bind(ParametersParser.class).in(Singleton.class);
        bind(new TypeLiteral<IValidator<String>>() { /**/ }).annotatedWith(Names.named(PARAMETERS)).to(ParametersParser.class);
        bind(new TypeLiteral<IParser<String, Parameters>>(){ /**/ }).to(ParametersParser.class);
        bind(ICodeCompletionTokenizer.class).to(CodeCompletionTokenizer.class).in(Singleton.class);
        bind(IJson.class).to(Json.class).in(Singleton.class);
        bind(new TypeLiteral<IValidator<String>>() { /**/ }).annotatedWith(Names.named(URL)).to(URLValidator.class).in(Singleton.class);
        bind(IContextSplitter.class).to(ContextSplitter.class).in(Singleton.class);
        bind(IHintTextBuilder.class).to(HintTextBuilder.class).in(Singleton.class);
        bind(IClock.class).to(Clock.class).in(Singleton.class);
        bind(IMath.class).to(Math.class).in(Singleton.class);
        bind(IInputDelayStatistics.class).to(InputDelayStatistics.class).in(Singleton.class);
        bind(IContextInitializer.class).to(ContextInitializer.class).in(Singleton.class);
        bind(IHistoricalHint.class).to(Hint.class);
        bind(IHintHistory.class).to(HintHistory.class);
        bind(CodeCompletionStatistics.class).in(Singleton.class);
        bind(ICodeCompletionContext.class).to(CodeCompletionStatistics.class);
        bind(ICodeCompletionStatistics.class).to(CodeCompletionStatistics.class);
        bind(ITextNormilizer.class).to(TextNormilizer.class).in(Singleton.class);
        bind(IStatistics.class).to(Statistics.class);
        bind(Contexts.class).in(Singleton.class);
        bind(ILocalContext.class).to(Contexts.class);
        bind(IGlobalContext.class).to(Contexts.class);
        bind(IJson.class).to(Json.class).in(Singleton.class);
        bind(IProgramingLanguage.class).to(ProgramingLanguage.class).in(Singleton.class);
        bind(IHashTools.class).to(HashTools.class).in(Singleton.class);
        bind(IIdProvider.class).to(HardwareIdProvider.class).in(Singleton.class);
        bind(IProposalExtractor.class).to(ProposalExtractor.class).in(Singleton.class);
        bind(IEnvironment.class).to(Environment.class).in(Singleton.class);
        bind(IMcpToolsCallMessageFactory.class).to(McpToolsCallMessageFactory.class).in(Singleton.class);
        bind(IMcpTools.class).to(McpTools.class).in(Singleton.class);
        bind(IClientTokenValidator.class).to(ClientTokenValidator.class).in(Singleton.class);
        // @formatter:on
    }
}
