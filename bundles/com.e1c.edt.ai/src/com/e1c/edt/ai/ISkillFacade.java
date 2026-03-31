/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai;

import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.assistent.SendUserMessageRequest;

/**
 * @author Bogdan Sushkov
 *
 */
public interface ISkillFacade
{
    CompletableFuture<String> execute(SendUserMessageRequest request, ICancellationToken token);
}
