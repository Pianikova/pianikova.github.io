/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;
import java.util.function.Function;

public interface ICodeCompletionStatistics
{
    <T> void addMethod(CodeMethod method, T state, Function<? super T, ? extends String> methodBodyProvider);

    Optional<String> getLastAcceptedSourceId();
}
