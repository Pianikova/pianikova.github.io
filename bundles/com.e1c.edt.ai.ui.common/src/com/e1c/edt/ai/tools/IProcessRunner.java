/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public interface IProcessRunner
{
    CompletableFuture<Optional<ProcessResult>> executeProcess(String executable, String workingDirectory,
        List<String> args, Long timeout, TimeUnit timeUnit);
}
