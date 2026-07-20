/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.assistent.model.Session;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Implementation of token validator service
 * @author Bogdan Sushkov
 *
 */
@Singleton
public class TokenValidator
    implements ITokenValidator
{
    @Inject
    private ISessionService sessionService;

    @Inject
    private ILog log;

    @Override
    public CompletableFuture<Boolean> validateTokenAsync()
    {
        return CompletableFuture.supplyAsync(() -> {
            try
            {
                CompletableFuture<Optional<Session>> future = sessionService.getGlobalSessionAsync();
                Optional<Session> session = future.get();

                // Token is considered valid if session exists and has a non-null sessionId
                return session.isPresent() && session.get().sessionId != null;
            }
            catch (Exception ex)
            {
                log.logError("Token validation failed: " + ex.getMessage()); //$NON-NLS-1$
                return false;
            }
        });
    }
}
