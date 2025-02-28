/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ServiceState;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IHealthCheckService
{
    CompletableFuture<ServiceState> checkAsync();
}
