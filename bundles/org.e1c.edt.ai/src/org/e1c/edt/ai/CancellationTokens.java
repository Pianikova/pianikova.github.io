/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.time.LocalDateTime;

import com.google.common.base.Preconditions;

public class CancellationTokens
{
    public final static ICancellationToken NONE = new ICancellationToken()
    {
        @Override
        public Boolean isCanceled()
        {
            return false;
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
            return cancellationToken.isCanceled() || clock.now().isAfter(expirationDate);
        }
    }
}