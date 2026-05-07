/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.skills;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IToolDirectiveExecutor
{
    CompletableFuture<String> executeAsync(CachedSkill skill, String toolId, Map<String, String> parameters,
        ICancellationToken cancellationToken);
}
