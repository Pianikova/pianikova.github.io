/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.assistent.model.Session;

public interface ISessionService
{
    CompletableFuture<Optional<Session>> getSessionAsync(IProject project);

    CompletableFuture<Optional<Session>> getGlobalSessionAsync();
}
