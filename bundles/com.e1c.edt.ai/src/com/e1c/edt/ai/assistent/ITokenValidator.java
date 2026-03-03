/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.concurrent.CompletableFuture;

/**
 * Service for validating client tokens
 * @author Bogdan Sushkov
 *
 */
public interface ITokenValidator
{
    /**
     * Validates the client token asynchronously
     * 
     * @return CompletableFuture with validation result (true if token is valid, false otherwise)
     */
    CompletableFuture<Boolean> validateTokenAsync();
}
