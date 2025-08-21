/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.assistent.model.Parameters;

public interface IParametersService
{
    CompletableFuture<Optional<Parameters>> getParametersAsync(boolean useCache);
}
