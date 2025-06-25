/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.Optional;

import com.e1c.edt.ai.ICancellationToken;

public interface IBmPovider
{
    Optional<BmRoot> getRoot(String filePath, ICancellationToken cancellationToken);
}
