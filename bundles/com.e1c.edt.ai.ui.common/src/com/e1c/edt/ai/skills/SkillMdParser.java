/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.e1c.edt.ai.assistent.SkillErrorCode;
import com.e1c.edt.ai.assistent.SkillExecutionException;
import com.e1c.edt.ai.assistent.model.SkillMetadata;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
@SuppressWarnings("nls")
public class SkillMdParser
    implements ISkillMdParser
{
    private final SkillTemplateProcessor skillTemplateProcessor;

    @Inject
    public SkillMdParser(SkillTemplateProcessor templateProcessor)
    {
        Preconditions.checkNotNull(templateProcessor);
        this.skillTemplateProcessor = templateProcessor;
    }

    @Override
    public CachedSkill parse(String skillId, String skillContent)
    {
        try {
            ParsedMarkdown parsedMarkdown = splitMdContentIntoMetadataAndBody(skillContent);

            Set<String> toolDirectives = skillTemplateProcessor.findToolIds(skillContent);

            return new CachedSkill(skillId, new SkillMetadata(parsedMarkdown.metadata), parsedMarkdown.body,
                toolDirectives, null);
        } catch (Exception e) {
            throw new SkillExecutionException(SkillErrorCode.SKILL_PARSE_ERROR,
                "Failed to parse SKILL.md for skill " + skillId, e);
        }
    }

    private ParsedMarkdown splitMdContentIntoMetadataAndBody(String skillContent)
    {
        if (skillContent == null || skillContent.isEmpty())
        {
            throw new SkillExecutionException(SkillErrorCode.SKILL_PARSE_ERROR, "SKILL.md is empty");
        }

        var normalizedContent = skillContent.replace("\r\n", "\n").replace("\r", "\n");

        if (!normalizedContent.startsWith("---"))
        {
            return new ParsedMarkdown(Collections.emptyMap(), normalizedContent);
        }

        int end = normalizedContent.indexOf("---", 4);
        if (end < 0)
        {
            throw new SkillExecutionException(SkillErrorCode.SKILL_PARSE_ERROR, "SKILL.md frontmatter is not closed");
        }
        var frontmatter = normalizedContent.substring(4, end);
        var body = normalizedContent.substring(end + 4).trim();
        return new ParsedMarkdown(parseMetadata(frontmatter), body);
    }

    private Map<String, String> parseMetadata(String frontmatter)
    {
        Map<String, String> metadata = new LinkedHashMap<>();
        if (frontmatter == null || frontmatter.isEmpty())
        {
            return metadata;
        }
        var lines = frontmatter.split("\n");
        for (var line : lines)
        {
            String trimmed = line.trim();
            if (trimmed.isEmpty())
            {
                continue;
            }
            var colonIndex = line.indexOf(":");
            if (colonIndex <= 0)
            {
                throw new SkillExecutionException(SkillErrorCode.SKILL_PARSE_ERROR,
                    "Invalid frontmatter line: " + line);
            }
            var key = line.substring(0, colonIndex).trim();
            var value = line.substring(colonIndex + 1).trim();
            metadata.put(key, value);
        }
        return metadata;
    }

    private class ParsedMarkdown
    {
        private final Map<String, String> metadata;
        private final String body;

        public ParsedMarkdown(Map<String, String> metadata, String body)
        {
            this.metadata = metadata;
            this.body = body;
        }
    }
}
