/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.nio.file.Path;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.assistent.model.SkillExecutionRequest;
import com.e1c.edt.ai.assistent.model.SkillExecutionResult;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillExecutor
    implements ISkillExecutor
{
    /** Request parameter carrying the project/working directory, used to resolve project overrides. */
    static final String WORKING_DIRECTORY = "working_directory"; //$NON-NLS-1$

    private final SkillTemplateRenderer renderer;
    private final SkillPackageLoader skillPackageLoader;

    @Inject
    public SkillExecutor(SkillTemplateRenderer renderer, SkillPackageLoader packageLoader)
    {
        Preconditions.checkNotNull(renderer);
        Preconditions.checkNotNull(packageLoader);
        this.skillPackageLoader = packageLoader;
        this.renderer = renderer;
    }

    @Override
    public CompletableFuture<SkillExecutionResult> executeAsync(SkillExecutionRequest request,
        ICancellationToken cancellationToken)
    {
        // Skills are loaded on explicit user actions, so we re-read on every execution rather than
        // cache: this guarantees that edits to .workmate overrides take effect immediately.
        var projectRoot = projectRootFromParameters(request.getParameters());
        var skill = skillPackageLoader.load(request.getSkillId(), projectRoot);

        return renderer.renderAsync(skill, request.getParameters(), cancellationToken)
            .thenApply(prompt -> new SkillExecutionResult(prompt,
                skill.getMetadata().getAllowedTools().orElse(null)));
    }

    private static Optional<Path> projectRootFromParameters(Map<String, String> parameters)
    {
        if (parameters == null)
        {
            return Optional.empty();
        }
        var workingDirectory = parameters.get(WORKING_DIRECTORY);
        if (workingDirectory == null || workingDirectory.isBlank())
        {
            return Optional.empty();
        }
        try
        {
            return Optional.of(Path.of(workingDirectory));
        }
        catch (Exception e)
        {
            return Optional.empty();
        }
    }
}
