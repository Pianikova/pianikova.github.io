/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.e1c.edt.ai.assistent.model.Parameters;

public interface IParametersService
{
    CompletableFuture<Optional<Parameters>> getParametersAsync();
}
