/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.e1c.edt.ai.assistent.model.Session;

public interface ISessionService
{
    CompletableFuture<Optional<Session>> getSessionAsync();
}
