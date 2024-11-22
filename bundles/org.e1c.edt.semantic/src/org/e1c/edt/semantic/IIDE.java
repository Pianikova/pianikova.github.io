/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;

interface IIDE
{
    Optional<CloseResponse> close(CloseRequest request,
        ICancellationToken cancellationToken);
}
