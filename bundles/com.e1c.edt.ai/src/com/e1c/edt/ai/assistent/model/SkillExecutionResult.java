/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.assistent.model;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillExecutionResult
{
    private final String prompt;
    private final Set<String> allowedTools;

    public SkillExecutionResult(String prompt)
    {
        this(prompt, null);
    }

    public SkillExecutionResult(String prompt, Set<String> allowedTools)
    {
        this.prompt = prompt;
        this.allowedTools = allowedTools == null ? null
            : Collections.unmodifiableSet(new LinkedHashSet<>(allowedTools));
    }

    public String getPrompt()
    {
        return prompt;
    }

    /**
     * @return the skill tool allowlist, or an empty optional when all tools are available
     */
    public Optional<Set<String>> getAllowedTools()
    {
        return Optional.ofNullable(allowedTools);
    }
}
