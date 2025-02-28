/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.semantic;

import java.util.Optional;

import com.e1c.edt.ai.ICancellationToken;

interface IIDE
{
    Optional<CloseResponse> close(CloseRequest request,
        ICancellationToken cancellationToken);
}
