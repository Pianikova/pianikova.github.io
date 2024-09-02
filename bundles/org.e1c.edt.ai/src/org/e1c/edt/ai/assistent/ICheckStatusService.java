/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.concurrent.CompletableFuture;

/**
 * @author Bogdan Sushkov
 *
 */
public interface ICheckStatusService
{
    CompletableFuture<Integer> getStatusAsync();
}
