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
    public static final Verbosity DEFAULT_VERBOSITY = Verbosity.WARNING;
    public static final int DEFAULT_GIT_CONTEXT_LINES = 8;
    public static final int DEAULT_TIMEOUT = 15000;
    public static final int DEFAULT_MIN_DELAY = 300;
    public static final int DEFAULT_PREFIX_LEN = 1000;
    public static final int DEFAULT_SUFFIX_LEN = 500;
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
            val -> parameters.prefixLength = Optional.of(Integer.parseInt(val))));

        names.remove(parse(properties, "suffix_length", validationResult,
            val -> parameters.suffixLength = Optional.of(Integer.parseInt(val))));

        names.remove(parse(properties, "form_length", validationResult,
            val -> parameters.formLength = Optional.of(Integer.parseInt(val))));

        names.remove(parse(properties, "meta_length", validationResult,
            val -> parameters.metaLength = Optional.of(Integer.parseInt(val))));

        names.remove(
            parse(properties, "best_of", validationResult, val -> parameters.bestOf = Optional.of(Integer.parseInt(val))));

        names.remove(parse(properties, "decoder_input_details", validationResult,
            val -> parameters.decoderInputDetails = parseBoolean(val)));

        names.remove(
            parse(properties, "do_sample", validationResult, val -> parameters.doSample = parseBoolean(val)));

        names.remove(parse(properties, "max_new_tokens", validationResult,
            val -> parameters.maxNewTokens = Optional.of(Integer.parseInt(val))));

        names.remove(parse(properties, "repetition_penalty", validationResult,
            val -> parameters.repetitionPenalty = Optional.of(Double.parseDouble(val))));

        names.remove(parse(properties, "frequency_penalty", validationResult,
            val -> parameters.frequencyPenalty = Optional.of(Double.parseDouble(val))));

        names.remove(parse(properties, "return_full_text", validationResult,
            val -> parameters.returnFullText = parseBoolean(val)));

        names.remove(parse(properties, "seed", validationResult, val -> parameters.seed = parseBoolean(val)));

        names.remove(parse(properties, "temperature", validationResult,
            val -> parameters.temperature = Optional.of(Double.parseDouble(val))));

        names.remove(parse(properties, "top_k", validationResult, val -> parameters.topK = Optional.of(Integer.parseInt(val))));

        names.remove(parse(properties, "top_n_tokens", validationResult,
            val -> parameters.topNTokens = Optional.of(Integer.parseInt(val))));

        names.remove(parse(properties, "top_p", validationResult, val -> parameters.topP = Optional.of(Double.parseDouble(val))));

        names
            .remove(parse(properties, "truncate", validationResult, val -> parameters.truncate = parseBoolean(val)));

        names.remove(
            parse(properties, "typical_p", validationResult,
                val -> parameters.typicalP = Optional.of(Double.parseDouble(val))));

        names.remove(
            parse(properties, "watermark", validationResult, val -> parameters.watermark = parseBoolean(val)));

        names.remove(
            parse(properties, "token_healing", validationResult,
                val -> {
                    var healing = parseEnum(val, TokenHealing.class);
                    switch (healing)
                    {
                    case NONE:
                        parameters.tokenHealing = Optional.empty();
                        break;
                    default:
                        parameters.tokenHealing = Optional.ofNullable(healing);
                        break;
                    }
                }));

        names.remove(
            parse(properties, "return_line", validationResult, val -> parameters.returnLine = parseBoolean(val)));

        names.remove(parse(properties, "trim_stop", validationResult, val -> parameters.trimStop = parseBoolean(val)));

        names.remove(parse(properties, "url", validationResult, val -> parameters.url = parseUrl(val)));

        names.remove(
            parse(properties, "chat_url", validationResult, val -> parameters.chatUrl = Optional.of(parseUrl(val))));

        names.remove(
            parse(properties, "update_url", validationResult, val -> parameters.updateUrl = Optional.ofNullable(val)));

        names.remove(parse(properties, "local_functions_length", validationResult,
            val -> parameters.localFunctionsLength = Optional.of(Integer.parseInt(val))));

        names.remove(parse(properties, "external_functions_length", validationResult,
            val -> parameters.externalFunctionsLength = Optional.of(Integer.parseInt(val))));

        names.remove(parse(properties, "global_meta_length", validationResult,
            val -> parameters.globalMetaLength = Optional.of(Integer.parseInt(val))));

        names.remove(parse(properties, "clipboard_length", validationResult,
            val -> parameters.clipboardLength = Optional.of(Integer.parseInt(val))));

        names.remove(
            parse(properties, "min_delay", validationResult,
                val -> {
                    if (val != null && !val.isBlank())
                    {
                        var minDelay = Integer.parseInt(val);
                        if (minDelay < 50)
                        {
                            validationResult.addError(new ValidationError(WellknownError.OutOfRange, "min_delay"));
                            minDelay = 50;
                        }

                        if (minDelay > 1000)
                        {
                            validationResult.addError(new ValidationError(WellknownError.OutOfRange, "min_delay"));
                            minDelay = 1000;
                        }

                        parameters.minDelay = Optional.of(minDelay);
                    }
                }));

        names.remove(parse(properties, "timeout", validationResult, val -> {
            if (val != null && !val.isBlank())
            {
                var timeout = Integer.parseInt(val);
                if (timeout < 50)
                {
                    validationResult.addError(new ValidationError(WellknownError.OutOfRange, "timeout"));
                    timeout = 50;
                }

                if (timeout > 60000)
                {
                    validationResult.addError(new ValidationError(WellknownError.OutOfRange, "timeout"));
                    timeout = 60000;
                }

                parameters.timeout = Optional.of(timeout);
            }
        }));

        names.remove(parse(properties, "global_context", validationResult,
            val -> parameters.globalContext = val == null || val.isBlank() ? null : parseBoolean(val)));

        names.remove(parse(properties, "experimental", validationResult,
            val -> parameters.experimental = val == null || val.isBlank() ? null : parseBoolean(val)));

        names.remove(parse(properties, "verbosity", validationResult,
            val -> parameters.verbosity =
                val == null || val.isBlank() ? DEFAULT_VERBOSITY : parseEnum(val, Verbosity.class)));

        names.remove(parse(properties, "resources", validationResult,
            val -> parameters.resources = val == null || val.isBlank() ? null : Optional.of(val.trim())));

        names.remove(parse(properties, "git_diff_context_lines", validationResult,
            val -> parameters.gitDiffContextLines =
                val == null || val.isBlank() ? null : Optional.of(Integer.parseInt(val))));

        names.remove(parse(properties, "instance_type", validationResult,
            val -> parameters.instanceType = val == null || val.isBlank() ? null : Optional.of(val.trim())));

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

    private Optional<Boolean> parseBoolean(String text)
    {
        if (text == null)
        {
            throw new IllegalArgumentException(text);
        }

        text = text.trim();
        if ("true".equalsIgnoreCase(text)) //$NON-NLS-1$
        {
            return Optional.of(true);
        }

        if ("false".equalsIgnoreCase(text)) //$NON-NLS-1$
        {
            return Optional.of(false);
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
