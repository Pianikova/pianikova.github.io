/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Optional;

import com.e1c.edt.ai.assistent.model.Parameters;

public interface IParser<TTarget, TResult>
{
    Optional<Parameters> parse(TTarget target);
}
