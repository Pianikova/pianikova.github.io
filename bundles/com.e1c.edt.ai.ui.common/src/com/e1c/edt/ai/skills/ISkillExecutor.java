/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.assistent.model.SkillExecutionRequest;
import com.e1c.edt.ai.assistent.model.SkillExecutionResult;

/**
 * @author Bogdan Sushkov
 *
 */
public interface ISkillExecutor
{
    CompletableFuture<SkillExecutionResult> executeAsync(SkillExecutionRequest request,
        ICancellationToken cancellationToken);
}
