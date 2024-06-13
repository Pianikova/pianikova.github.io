/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import org.e1c.edt.ai.assistent.AICodeAssistant;
import org.e1c.edt.ai.assistent.IAICodeAssistant;
import org.e1c.edt.ai.assistent.IResponseLineProcessor;
import org.e1c.edt.ai.assistent.IResponseStreamProcessor;
import org.e1c.edt.ai.assistent.ResponseLineProcessor;
import org.e1c.edt.ai.assistent.ResponseStreamProcessor;
import org.e1c.edt.ai.assistent.model.Parameters;

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
        bind(IAICodeAssistant.class).to(AICodeAssistant.class).in(Singleton.class);
        bind(new TypeLiteral<IValidator<String>>() { /**/ }).annotatedWith(Names.named(URL)).to(URLValidator.class).in(Singleton.class);
        bind(IAIContextSplitter.class).to(AIContextSplitter.class).in(Singleton.class);
        bind(IHintTextBuilder.class).to(HintTextBuilder.class).in(Singleton.class);
        bind(IClock.class).to(Clock.class).in(Singleton.class);
        bind(IMath.class).to(Math.class).in(Singleton.class);
        bind(IInputDelayStatistics.class).to(InputDelayStatistics.class).in(Singleton.class);
        bind(IAIContextFactory.class).to(AIContextFactory.class).in(Singleton.class);
        bind(IStringNormalizer.class).to(StringNormalizer.class).in(Singleton.class);
        // @formatter:on
    }
}
