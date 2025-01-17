/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.assistent.model.TokenHealing;

import com.google.common.base.Preconditions;

public class ParametersParser
    implements IValidator<String>, IParser<String, Parameters>
{
    private final static String keyValueSeparator = ";"; //$NON-NLS-1$

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
        var parameters = new Parameters();
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

        names.remove(parse(properties, "chat_url", validationResult, val -> parameters.chatUrl = val));

        names.remove(parse(properties, "local_functions_length", validationResult,
            val -> parameters.localFunctionsLength = Integer.parseInt(val)));

        names.remove(parse(properties, "external_functions_length", validationResult,
            val -> parameters.externalFunctionsLength = Integer.parseInt(val)));

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
}
