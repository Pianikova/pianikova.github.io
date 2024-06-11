/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.CancellationToken;
import org.e1c.edt.ai.IObserver;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IAICodeAssistant
{
    public Optional<CompletableFuture<Void>> generate(AIContext aiContext, IObserver<String> observer,
        CancellationToken cancellationToken);
}
