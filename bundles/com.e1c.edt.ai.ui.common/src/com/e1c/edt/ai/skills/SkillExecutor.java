/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

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
    private final ISkillCache cache;
    private final SkillTemplateRenderer renderer;
    private final SkillPackageLoader skillPackageLoader;

    @Inject
    public SkillExecutor(ISkillCache cache, SkillTemplateRenderer renderer, SkillPackageLoader packageLoader)
    {
        Preconditions.checkNotNull(cache);
        Preconditions.checkNotNull(renderer);
        Preconditions.checkNotNull(packageLoader);
        this.skillPackageLoader = packageLoader;
        this.cache = cache;
        this.renderer = renderer;
    }

    @Override
    public CompletableFuture<SkillExecutionResult> executeAsync(SkillExecutionRequest request,
        ICancellationToken cancellationToken)
    {
        var skillId = request.getSkillId();
        var skill = cache.computeIfAbsent(skillId, () -> skillPackageLoader.load(skillId));

        return renderer.renderAsync(skill, request.getParameters(), cancellationToken)
            .thenApply(SkillExecutionResult::new);
    }

}
