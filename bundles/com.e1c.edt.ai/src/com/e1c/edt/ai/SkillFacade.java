/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai;

import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.assistent.SendUserMessageRequest;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class SkillFacade
    implements ISkillFacade
{
    private final ISkillRegistry skillRegistry;

    @Inject
    public SkillFacade(ISkillRegistry skillRegistry)
    {
        Preconditions.checkNotNull(skillRegistry);
        this.skillRegistry = skillRegistry;
    }

    @Override
    public CompletableFuture<String> execute(SendUserMessageRequest request, ICancellationToken cancellationToken)
    {
        var skillId = request.getSkillId();
        if (skillId == null || skillId.isBlank())
        {
            return CompletableFuture.failedFuture(new IllegalArgumentException("Skill id is not specified")); //$NON-NLS-1$
        }
        var skill = skillRegistry.findById(skillId)
            .orElseThrow(() -> new IllegalArgumentException("Skill not found: " + skillId)); //$NON-NLS-1$
        return skill.prepareAsync(request, cancellationToken);
    }
}
