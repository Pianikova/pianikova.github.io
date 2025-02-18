/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.concurrent.CompletableFuture;

import org.e1c.edt.ai.ServiceState;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IHealthCheckService
{
    CompletableFuture<ServiceState> checkAsync();
}
