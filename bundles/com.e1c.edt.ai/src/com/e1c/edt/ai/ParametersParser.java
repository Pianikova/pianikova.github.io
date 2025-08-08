/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.io.IOException;
import java.io.StringReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import com.e1c.edt.ai.assistent.model.Parameters;
import com.e1c.edt.ai.assistent.model.TokenHealing;
import com.e1c.edt.ai.assistent.model.Verbosity;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ParametersParser
    implements IValidator<String>, IParser<String, Parameters>
{
    private final static String keyValueSeparator = ";"; //$NON-NLS-1$
    private final IDefaultSettings defaultSettings;

    @Inject
    public ParametersParser(IDefaultSettings defaultSettings)
    {
        Preconditions.checkNotNull(defaultSettings);
        this.defaultSettings = defaultSettings;
    }

    @Override
    public ValidationResult validate(String target)
    {
        Preconditions.checkNotNull(target);
        var validationResult = new ValidationResult();
        parse(target, validationResult);
        return validationResult;
    }

    @Override
    public Optional<Parameters> parse(String target)
    {
        Preconditions.checkNotNull(target);
        return parse(target, new ValidationResult());
    }

    @SuppressWarnings("nls")
    public Optional<Parameters> parse(String parametersText, ValidationResult validationResult)
    {
        Preconditions.checkNotNull(parametersText);
        Preconditions.checkNotNull(validationResult);
        var parameters = new Parameters(defaultSettings);
        if (parametersText.isBlank())
        {
            return Optional.of(parameters);
        }

        var reader = new StringReader(parametersText.replace(keyValueSeparator, System.lineSeparator()));
        var properties = new Properties();
        try
        {
            properties.load(reader);
        }
        catch (IOException e)
        {
            validationResult.addError(new ValidationError(WellknownError.UnableToParse, parametersText));
            return Optional.empty();
        }

        var names = new HashSet<>(properties.stringPropertyNames());

        names.remove(parse(properties, "prefix_length", validationResult,
            val -> parameters.prefixLength = Integer.parseInt(val)));

        names.remove(parse(properties, "suffix_length", validationResult,
            val -> parameters.suffixLength = Integer.parseInt(val)));

        names.remove(parse(properties, "form_length", validationResult,
            val -> parameters.formLength = Integer.parseInt(val)));

        names.remove(parse(properties, "meta_length", validationResult,
            val -> parameters.metaLength = Integer.parseInt(val)));

        names.remove(
            parse(properties, "best_of", validationResult, val -> parameters.bestOf = Integer.parseInt(val)));

        names.remove(parse(properties, "decoder_input_details", validationResult,
            val -> parameters.decoderInputDetails = parseBoolean(val)));

        names.remove(
            parse(properties, "do_sample", validationResult, val -> parameters.doSample = parseBoolean(val)));

        names.remove(parse(properties, "max_new_tokens", validationResult,
            val -> parameters.maxNewTokens = Integer.parseInt(val)));

        names.remove(parse(properties, "repetition_penalty", validationResult,
            val -> parameters.repetitionPenalty = Double.parseDouble(val)));

        names.remove(parse(properties, "frequency_penalty", validationResult,
            val -> parameters.frequencyPenalty = Double.parseDouble(val)));

        names.remove(parse(properties, "return_full_text", validationResult,
            val -> parameters.returnFullText = parseBoolean(val)));

        names.remove(parse(properties, "seed", validationResult, val -> parameters.seed = parseBoolean(val)));

        names.remove(parse(properties, "temperature", validationResult,
            val -> parameters.temperature = Double.parseDouble(val)));

        names.remove(parse(properties, "top_k", validationResult, val -> parameters.topK = Integer.parseInt(val)));

        names.remove(parse(properties, "top_n_tokens", validationResult,
            val -> parameters.topNTokens = Integer.parseInt(val)));

        names.remove(parse(properties, "top_p", validationResult, val -> parameters.topP = Double.parseDouble(val)));

        names
            .remove(parse(properties, "truncate", validationResult, val -> parameters.truncate = parseBoolean(val)));

        names.remove(
            parse(properties, "typical_p", validationResult, val -> parameters.typicalP = Double.parseDouble(val)));

        names.remove(
            parse(properties, "watermark", validationResult, val -> parameters.watermark = parseBoolean(val)));

        names.remove(
            parse(properties, "token_healing", validationResult,
                val -> parameters.tokenHealing = parseEnum(val, TokenHealing.class)));

        names.remove(
            parse(properties, "return_line", validationResult, val -> parameters.returnLine = parseBoolean(val)));

        names.remove(parse(properties, "trim_stop", validationResult, val -> parameters.trimStop = parseBoolean(val)));

        names.remove(parse(properties, "url", validationResult, val -> parameters.url = parseUrl(val)));

        names.remove(parse(properties, "chat_url", validationResult, val -> parameters.chatUrl = parseUrl(val)));

        names.remove(parse(properties, "update_url", validationResult, val -> parameters.updateUrl = val));

        names.remove(parse(properties, "local_functions_length", validationResult,
            val -> parameters.localFunctionsLength = Integer.parseInt(val)));

        names.remove(parse(properties, "external_functions_length", validationResult,
            val -> parameters.externalFunctionsLength = Integer.parseInt(val)));

        names.remove(
            parse(properties, "min_delay", validationResult,
                val -> {
                    parameters.minDelay = Integer.parseInt(val);
                    if (parameters.minDelay < 50)
                    {
                        validationResult.addError(new ValidationError(WellknownError.OutOfRange, "min_delay"));
                        parameters.minDelay = 50;
                    }

                    if (parameters.minDelay > 1000)
                    {
                        validationResult.addError(new ValidationError(WellknownError.OutOfRange, "min_delay"));
                        parameters.minDelay = 1000;
                    }
                }));

        names.remove(parse(properties, "timeout", validationResult, val -> {
            parameters.timeout = Integer.parseInt(val);
            if (parameters.timeout < 50)
            {
                validationResult.addError(new ValidationError(WellknownError.OutOfRange, "timeout"));
                parameters.timeout = 50;
            }

            if (parameters.timeout > 60000)
            {
                validationResult.addError(new ValidationError(WellknownError.OutOfRange, "timeout"));
                parameters.timeout = 60000;
            }
        }));

        names.remove(parse(properties, "global_context", validationResult,
            val -> parameters.globalContext = parseBoolean(val)));

        names.remove(parse(properties, "extended_context", validationResult,
            val -> parameters.extendedContext = parseBoolean(val)));

        names.remove(parse(properties, "verbosity", validationResult,
            val -> parameters.verbosity = parseEnum(val, Verbosity.class)));

        names.remove(parse(properties, "resources", validationResult, val -> parameters.resources = val.trim()));

        names.remove(parse(properties, "git_diff_context_lines", validationResult,
            val -> parameters.gitDiffContextLines = Integer.parseInt(val)));

        var unknowNames = new ArrayList<>(names);
        unknowNames.sort(null);
        for (var unknowName : unknowNames)
        {
            validationResult.addError(new ValidationError(WellknownError.Unknown, unknowName));
        }

        return Optional.of(parameters);
    }

    private String parse(Properties properties, String name, ValidationResult validationResult,
        Consumer<String> valueConsumer)
    {
        var val = properties.getProperty(name);
        if (val == null)
        {
            return name;
        }

        try
        {
            val = val.trim();
            valueConsumer.accept(val);
            return name;
        }
        catch (Exception ex)
        {
            // ignored
        }

        validationResult.addError(new ValidationError(WellknownError.UnableToParse, name));
        return name;
    }

    private Boolean parseBoolean(String text)
    {
        if (text == null)
        {
            throw new IllegalArgumentException(text);
        }

        text = text.trim();
        if ("true".equalsIgnoreCase(text)) //$NON-NLS-1$
        {
            return true;
        }

        if ("false".equalsIgnoreCase(text)) //$NON-NLS-1$
        {
            return false;
        }

        throw new IllegalArgumentException(text);
    }

    private <TEnum extends Enum<TEnum>> TEnum parseEnum(String text, Class<TEnum> clazz)
    {
        if (text == null)
        {
            throw new IllegalArgumentException(text);
        }

        for (TEnum value : EnumSet.allOf(clazz))
        {
            if (value.name().equalsIgnoreCase(text))
            {
                return value;
            }
        }

        throw new IllegalArgumentException(text);
    }

    private URL parseUrl(String text)
    {
        if (text == null)
        {
            throw new IllegalArgumentException(text);
        }

        try
        {
            return new URL(normalizeUrl(text));
        }
        catch (MalformedURLException e)
        {
            throw new IllegalArgumentException(text);
        }
    }

    private String normalizeUrl(String text)
    {
        if (text.isEmpty())
        {
            return text;
        }

        text = text.trim();
        if (!text.endsWith("/")) //$NON-NLS-1$
        {
            text = text + "/"; //$NON-NLS-1$
        }

        return text;
    }
}
