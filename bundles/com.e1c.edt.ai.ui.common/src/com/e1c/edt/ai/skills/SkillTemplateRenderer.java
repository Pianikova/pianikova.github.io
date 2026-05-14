/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.assistent.SkillErrorCode;
import com.e1c.edt.ai.assistent.SkillExecutionException;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillTemplateRenderer
{
    private final SkillTemplateProcessor templateProcessor;
    private final IToolDirectiveExecutor toolDirectiveExecutor;

    @Inject
    public SkillTemplateRenderer(SkillTemplateProcessor templateProcessor, IToolDirectiveExecutor toolDirectiveExecutor)
    {
        Preconditions.checkNotNull(templateProcessor);
        Preconditions.checkNotNull(toolDirectiveExecutor);
        this.templateProcessor = templateProcessor;
        this.toolDirectiveExecutor = toolDirectiveExecutor;
    }

    public CompletableFuture<String> renderAsync(CachedSkill skill, Map<String, String> parameters,
        ICancellationToken cancellationToken)
    {
        // insert ${...} into the md-body
        String resolvedTemplate = templateProcessor.resolvePlaceholders(skill.getTemplate(), parameters);

        // execute tools and insert their results into the md-body
        var futures = new LinkedHashMap<String, CompletableFuture<String>>();

        for (var toolId: skill.getToolIds()) {
            futures.computeIfAbsent(toolId,
                id -> toolDirectiveExecutor.executeAsync(skill, id, parameters, cancellationToken));
        }

        CompletableFuture<?>[] futureArray = futures.values().toArray(new CompletableFuture[0]);

        return CompletableFuture.allOf(futureArray).thenApply(ignored -> {
            Map<String, String> toolResults = new HashMap<>();

            for (Map.Entry<String, CompletableFuture<String>> entry : futures.entrySet())
            {
                toolResults.put(entry.getKey(), entry.getValue().join());
            }

            return toolResults;
        })
            .thenApply(toolResults -> templateProcessor.replaceToolResults(resolvedTemplate, toolResults))
            .exceptionally(error -> {
            throw new SkillExecutionException(SkillErrorCode.TOOL_EXECUTION_ERROR,
                "Failed to render skill template: " + skill.getSkillId(), error); //$NON-NLS-1$
        });
    }
}
