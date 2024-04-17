/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.function.Consumer;

import org.e1c.edt.ai.assistent.model.Parameters;

public class ParametersParser
    implements IValidator<String>, IParser<String, Parameters>
{
    private final static String keyValueSeparator = ";"; //$NON-NLS-1$

    @Override
    public ValidationResult validate(String target)
    {
        var validationResult = new ValidationResult();
        tryParse(target, validationResult);
        return validationResult;
    }

    @Override
    public Parameters parse(String target)
    {
        return tryParse(target, new ValidationResult());
    }

    @SuppressWarnings("nls")
    public Parameters tryParse(String parametersText, ValidationResult validationResult)
    {
        var parameters = new Parameters();
        var reader = new StringReader(parametersText.replace(keyValueSeparator, System.lineSeparator()));
        var properties = new Properties();
        try
        {
            properties.load(reader);
        }
        catch (IOException e)
        {
            validationResult.addError(new ValidationError(WellknownError.UnableToParse, parametersText));
            return parameters;
        }

        var names = new HashSet<>(properties.stringPropertyNames());

        names.remove(
            tryParse(properties, "best_of", validationResult, val -> parameters.bestOf = Integer.parseInt(val)));

        names.remove(tryParse(properties, "decoder_input_details", validationResult,
            val -> parameters.decoderInputDetails = parseBoolean(val)));

        names.remove(
            tryParse(properties, "do_sample", validationResult, val -> parameters.doSample = parseBoolean(val)));

        names.remove(tryParse(properties, "max_new_tokens", validationResult,
            val -> parameters.maxNewTokens = Integer.parseInt(val)));

        names.remove(tryParse(properties, "repetition_penalty", validationResult,
            val -> parameters.repetitionPenalty = Double.parseDouble(val)));

        names.remove(tryParse(properties, "frequency_penalty", validationResult,
            val -> parameters.frequencyPenalty = Double.parseDouble(val)));

        names.remove(tryParse(properties, "return_full_text", validationResult,
            val -> parameters.returnFullText = parseBoolean(val)));

        names.remove(tryParse(properties, "seed", validationResult, val -> parameters.seed = parseBoolean(val)));

        names.remove(tryParse(properties, "temperature", validationResult,
            val -> parameters.temperature = Double.parseDouble(val)));

        names.remove(tryParse(properties, "top_k", validationResult, val -> parameters.topK = Integer.parseInt(val)));

        names.remove(tryParse(properties, "top_n_tokens", validationResult,
            val -> parameters.topNTokens = Integer.parseInt(val)));

        names.remove(tryParse(properties, "top_p", validationResult, val -> parameters.topP = Double.parseDouble(val)));

        names
            .remove(tryParse(properties, "truncate", validationResult, val -> parameters.truncate = parseBoolean(val)));

        names.remove(
            tryParse(properties, "typical_p", validationResult, val -> parameters.typicalP = Double.parseDouble(val)));

        names.remove(
            tryParse(properties, "watermark", validationResult, val -> parameters.watermark = parseBoolean(val)));

        var unknowNames = new ArrayList<>(names);
        unknowNames.sort(null);
        for (var unknowName : unknowNames)
        {
            validationResult.addError(new ValidationError(WellknownError.Unknown, unknowName));
        }

        return parameters;
    }

    private String tryParse(Properties properties, String name, ValidationResult validationResult,
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
}
