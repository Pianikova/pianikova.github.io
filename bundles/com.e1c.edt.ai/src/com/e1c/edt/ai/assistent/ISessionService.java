/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.assistent.model.ProjectId;
import com.e1c.edt.ai.assistent.model.Session;

public interface ISessionService
{
    CompletableFuture<Optional<Session>> getSessionAsync(ProjectId projectId);
}
