/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

import org.e1c.edt.ai.assistent.model.Parameters;

public interface IParser<TTarget, TResult>
{
    Optional<Parameters> parse(TTarget target);
}
