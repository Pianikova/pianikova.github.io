/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.function.Consumer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;
import org.mockito.Mockito;
import org.osgi.framework.Version;

import com.e1c.edt.ai.assistent.model.TokenHealing;
import com.e1c.edt.ai.assistent.model.Verbosity;

@RunWith(Parameterized.class)
public class ParametersParserTest
{
    private static final IDefaultSettings defaultSettings = Mockito.mock(IDefaultSettings.class);

    @Parameter(0)
    public String parametersText;

    @Parameter(1)
    public ValidationResult expectedValidationResult;

    @Parameter(2)
    public Optional<com.e1c.edt.ai.assistent.model.Parameters> expectedParameters;

    static
    {
        when(defaultSettings.getUrl()).thenReturn("http://abc.ru"); //$NON-NLS-1$
    }

    @Test
    @Parameters()
    public void shouldParseParameterFromText()
    {
        // Given
        var parser = new ParametersParser(defaultSettings);
        var actualValidationResult = new ValidationResult();

        // When
        var actualParameters = parser.parse(parametersText, actualValidationResult);

        // Then
        Assert.assertEquals(expectedValidationResult, actualValidationResult);
        Assert.assertEquals(expectedParameters.hashCode(), actualParameters.hashCode());
        Assert.assertEquals(expectedParameters, actualParameters);
    }

    @SuppressWarnings("nls")
    @Parameters(name = "{index}: {0}")
    public static Collection<Object[]> data()
    {
        // @formatter:off
        var defaultParams = createParams(i -> { /**/ });
        return Arrays.asList(
            new Object[][] {
                {"", ValidationResult.SUCCESS, defaultParams },
                { "best_of=9", ValidationResult.SUCCESS, createParams(p -> { p.bestOf = 9; } ) },
                { "best_of =  9", ValidationResult.SUCCESS, createParams(p -> { p.bestOf = 9; } ) },
                { "best_of=Abc", new ValidationResult(UnableToParse("best_of")), defaultParams },
                { "decoder_input_details=True", ValidationResult.SUCCESS, createParams(p -> { p.decoderInputDetails = true; } ) },
                { "decoder_input_details=true", ValidationResult.SUCCESS, createParams(p -> { p.decoderInputDetails = true; } ) },
                { "decoder_input_details=False", ValidationResult.SUCCESS, createParams(p -> { p.decoderInputDetails = false; } ) },
                { "decoder_input_details=false", ValidationResult.SUCCESS, createParams(p -> { p.decoderInputDetails = false; } ) },
                { "decoder_input_details=Abc", new ValidationResult(UnableToParse("decoder_input_details")), defaultParams },
                { "decoder_input_details=", new ValidationResult(UnableToParse("decoder_input_details")), defaultParams },
                { "best_of=9;decoder_input_details=True", ValidationResult.SUCCESS, createParams(p -> { p.bestOf = 9; p.decoderInputDetails = true; } ) },
                { "best_of = 9 ;  decoder_input_details=True", ValidationResult.SUCCESS, createParams(p -> { p.bestOf = 9; p.decoderInputDetails = true; } ) },
                { "xyz=Abc", new ValidationResult(Unknown("xyz")), defaultParams },
                { "xyz=Abc ; Ddd=ggg", new ValidationResult(Unknown("Ddd"), Unknown("xyz")), defaultParams },
                { "do_sample=true", ValidationResult.SUCCESS, createParams(p -> { p.doSample = true; } ) },
                { "max_new_tokens=77", ValidationResult.SUCCESS, createParams(p -> { p.maxNewTokens = 77; } ) },
                { "repetition_penalty=34.5", ValidationResult.SUCCESS, createParams(p -> { p.repetitionPenalty = 34.5; } ) },
                { "frequency_penalty=34.5", ValidationResult.SUCCESS, createParams(p -> { p.frequencyPenalty = 34.5; } ) },
                { "return_full_text=true", ValidationResult.SUCCESS, createParams(p -> { p.returnFullText = true; } ) },
                { "seed=true", ValidationResult.SUCCESS, createParams(p -> { p.seed = true; } ) },
                { "temperature=34.5", ValidationResult.SUCCESS, createParams(p -> { p.temperature = 34.5; } ) },
                { "top_k=34", ValidationResult.SUCCESS, createParams(p -> { p.topK = 34; } ) },
                { "top_n_tokens=34", ValidationResult.SUCCESS, createParams(p -> { p.topNTokens = 34; } ) },
                { "top_p=34.5", ValidationResult.SUCCESS, createParams(p -> { p.topP = 34.5; } ) },
                { "truncate=true", ValidationResult.SUCCESS, createParams(p -> { p.truncate = true; } ) },
                { "typical_p=34.5", ValidationResult.SUCCESS, createParams(p -> { p.typicalP = 34.5; } ) },
                { "watermark=true", ValidationResult.SUCCESS, createParams(p -> { p.watermark = true; } ) },
                { "token_healing=guidance", ValidationResult.SUCCESS, createParams(p -> { p.tokenHealing = TokenHealing.GUIDANCE; } ) },
                { "token_healing=streaming", ValidationResult.SUCCESS, createParams(p -> { p.tokenHealing = TokenHealing.STREAMING; } ) },
                { "token_healing=None", ValidationResult.SUCCESS, createParams(p -> { p.tokenHealing = TokenHealing.NONE; } ) },
                { "return_line=true", ValidationResult.SUCCESS, createParams(p -> { p.returnLine = true; } ) },
                { "trim_stop=true", ValidationResult.SUCCESS, createParams(p -> { p.trimStop = true; } ) },
                { "chat_url=http://chat.com/Abc", ValidationResult.SUCCESS, createParams(p -> {
                    try
                    {
                        p.chatUrl = new URL("http://chat.com/Abc/");
                    }
                    catch (MalformedURLException e)
                    {
                        //
                    } } )
                },
                { "chat_url=http://chat.com/Abc/", ValidationResult.SUCCESS, createParams(p -> {
                    try
                    {
                        p.chatUrl = new URL("http://chat.com/Abc/");
                    }
                    catch (MalformedURLException e)
                    {
                        //
                    } } )
                },
                { "url=http://Xyz.com/Abc", ValidationResult.SUCCESS, createParams(p -> {
                    try
                    {
                        p.url = new URL("http://Xyz.com/Abc/");
                    }
                    catch (MalformedURLException e)
                    {
                        //
                    } } )
                },
                { "url=http://Xyz.com/Abc/", ValidationResult.SUCCESS, createParams(p -> {
                    try
                    {
                        p.url = new URL("http://Xyz.com/Abc/");
                    }
                    catch (MalformedURLException e)
                    {
                        //
                    } } )
                },
                { "min_delay=300", ValidationResult.SUCCESS, createParams(p -> { p.minDelay = 300; } ) },
                { "timeout=15000", ValidationResult.SUCCESS, createParams(p -> { p.timeout = 15000; } ) },
                { "global_context=true", ValidationResult.SUCCESS, createParams(p -> { p.globalСontext = true; } ) },
                { "extended_context=true", ValidationResult.SUCCESS, createParams(p -> { p.extendedСontext = true; } ) },
                { "trace=true", ValidationResult.SUCCESS, createParams(p -> { p.trace = true; } ) },
                { "verbosity=detailed", ValidationResult.SUCCESS, createParams(p -> { p.verbosity = Verbosity.DETAILED; } ) },
                { "script_language=Russian", ValidationResult.SUCCESS, createParams(p -> { p.scriptLanguage = "russian"; } ) },
                { "script_language=enGlish", ValidationResult.SUCCESS, createParams(p -> { p.scriptLanguage = "english"; } ) },
                { "configuration_name=Abc", ValidationResult.SUCCESS, createParams(p -> { p.configurationName = "Abc"; } ) },
                { "version=1.2.3", ValidationResult.SUCCESS, createParams(p -> { p.version = Version.parseVersion("1.2.3"); } ) },
                { "vendor=Abc", ValidationResult.SUCCESS, createParams(p -> { p.vendor = "Abc"; } ) },
            });
        // @formatter:on
    }

    private static Optional<com.e1c.edt.ai.assistent.model.Parameters> createParams(
        Consumer<com.e1c.edt.ai.assistent.model.Parameters> parametersConsumer)
    {
        var parameters = new com.e1c.edt.ai.assistent.model.Parameters(defaultSettings);
        parametersConsumer.accept(parameters);
        return Optional.of(parameters);
    }

    private static ValidationError UnableToParse(String name)
    {
        return new ValidationError(WellknownError.UnableToParse, name);
    }

    private static ValidationError Unknown(String name)
    {
        return new ValidationError(WellknownError.Unknown, name);
    }
}
