/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic.handlers;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;

public interface IIDE
{
    Optional<CloseResponse> close(CloseRequest request,
        ICancellationToken cancellationToken);
}
