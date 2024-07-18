/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent;

import java.util.concurrent.CompletableFuture;

public interface IFeedbackService
{
    CompletableFuture<Void> acceptedCodeAsync(String uuid, String code);
}
