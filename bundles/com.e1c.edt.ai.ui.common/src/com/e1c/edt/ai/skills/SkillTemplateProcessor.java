/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.RegexTemplateProcessor;
import com.e1c.edt.ai.assistent.SkillErrorCode;
import com.e1c.edt.ai.assistent.SkillExecutionException;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillTemplateProcessor
{
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile("\\$\\{([a-zA-Z0-9._-]+)}"); //$NON-NLS-1$
    private static final Pattern TOOL_DIRECTIVE_PATTERN = Pattern.compile("!tool\\('([a-zA-Z0-9._-]+)'\\)"); //$NON-NLS-1$

    private final RegexTemplateProcessor templateProcessor;
    private final IJson json;

    @Inject
    public SkillTemplateProcessor(RegexTemplateProcessor templateProcessor, IJson json)
    {
        Preconditions.checkNotNull(templateProcessor);
        Preconditions.checkNotNull(json);
        this.json = json;
        this.templateProcessor = templateProcessor;
    }

    public Set<String> findToolIds(String text)
    {
        return templateProcessor.find(text, TOOL_DIRECTIVE_PATTERN);
    }

    public String resolvePlaceholders(String text, Map<String, String> parameters)
    {
        return templateProcessor.replace(text, PLACEHOLDER_PATTERN, match -> {
            var key = match.group(1);
            if (!parameters.containsKey(key)) {
                throw new SkillExecutionException(SkillErrorCode.MISSING_PARAMETER, "Missing skill parameter: " + key); //$NON-NLS-1$
            }
            var value = parameters.get(key);
            if (value == null)
            {
                throw new SkillExecutionException(SkillErrorCode.MISSING_PARAMETER, "Null skill parameter for key: " + key); //$NON-NLS-1$
            }
            return value;
        });
    }

    public String resolveJsonPlaceholders(String text, Map<String, String> parameters)
    {
        return templateProcessor.replace(text, PLACEHOLDER_PATTERN, match -> {
            var key = match.group(1);
            if (!parameters.containsKey(key))
            {
                throw new SkillExecutionException(SkillErrorCode.MISSING_PARAMETER, "Missing skill parameter: " + key); //$NON-NLS-1$
            }
            var value = parameters.get(key);
            if (value == null)
            {
                throw new SkillExecutionException(SkillErrorCode.MISSING_PARAMETER,
                    "Null skill parameter for key: " + key); //$NON-NLS-1$
            }
            var jsonString = json.serialize(value);
            return jsonString.substring(1, jsonString.length() - 1);
        });
    }

    public String replaceToolResults(String text, Map<String, String> toolResults)
    {
        return templateProcessor.replace(text, TOOL_DIRECTIVE_PATTERN, match -> {
            var toolId = match.group(1);
            if (!toolResults.containsKey(toolId))
            {
                throw new SkillExecutionException(SkillErrorCode.TOOL_EXECUTION_ERROR,
                    "Missing tool result: " + toolId); //$NON-NLS-1$
            }
            return toolResults.get(toolId);
        });
    }
}
