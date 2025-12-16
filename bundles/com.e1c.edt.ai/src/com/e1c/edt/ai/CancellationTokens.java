/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.time.LocalDateTime;
import java.util.function.Supplier;

import com.google.common.base.Preconditions;

public class CancellationTokens
{
    public static boolean isStopped = false;

    public final static ICancellationToken NONE = new ICancellationToken()
    {
        @Override
        public Boolean isCanceled()
        {
            return isStopped;
        }
    };

    public static ICancellationToken expiresAt(ICancellationToken cancellationToken, IClock clock,
        LocalDateTime expirationDate)
    {
        Preconditions.checkNotNull(cancellationToken);
        Preconditions.checkNotNull(clock);
        Preconditions.checkNotNull(expirationDate);
        return new ExpiringCancellationToken(cancellationToken, clock, expirationDate);
    }

    public static ICancellationToken manual(ICancellationToken cancellationToken, Supplier<Boolean> cancellationCheck)
    {
        Preconditions.checkNotNull(cancellationToken);
        Preconditions.checkNotNull(cancellationCheck);
        return new ManualCancellationToken(cancellationToken, cancellationCheck);
    }

    private static class ExpiringCancellationToken
        implements ICancellationToken
    {
        private final ICancellationToken cancellationToken;
        private final IClock clock;
        private final LocalDateTime expirationDate;

        public ExpiringCancellationToken(ICancellationToken cancellationToken, IClock clock,
            LocalDateTime expirationDate)
        {
            this.cancellationToken = cancellationToken;
            this.clock = clock;
            this.expirationDate = expirationDate;
        }

        @Override
        public Boolean isCanceled()
        {
            return isStopped || cancellationToken.isCanceled() || clock.now().isAfter(expirationDate);
        }
    }

    private static class ManualCancellationToken
        implements ICancellationToken
    {
        private final ICancellationToken cancellationToken;
        private final Supplier<Boolean> cancellationCheck;

        public ManualCancellationToken(ICancellationToken cancellationToken, Supplier<Boolean> cancellationCheck)
        {
            this.cancellationToken = cancellationToken;
            this.cancellationCheck = cancellationCheck;
        }

        @Override
        public Boolean isCanceled()
        {
            return isStopped || cancellationToken.isCanceled() || cancellationCheck.get();
        }
    }
}