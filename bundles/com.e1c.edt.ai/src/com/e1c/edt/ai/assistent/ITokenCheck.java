/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.concurrent.CompletableFuture;

public interface ITokenCheck
{
    CompletableFuture<Boolean> checkTokenAsync(String token);
}
