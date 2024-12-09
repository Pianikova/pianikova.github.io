/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.assistent.model.LocalContext;

public interface ILocalContextProvider
{
    public Optional<LocalContext> get(IStatistics statistics, ICancellationToken cancellationToken);
}
