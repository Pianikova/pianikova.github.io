/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.assistent.model.Session;

public interface IResponseCache
{
    CompletableFuture<Optional<Session>> get(IProject project,
        Supplier<CompletableFuture<Optional<Session>>> taskSupplier, boolean reset);

    CompletableFuture<Optional<Session>> getGlobal(Supplier<CompletableFuture<Optional<Session>>> taskSupplier,
        boolean reset);
}

