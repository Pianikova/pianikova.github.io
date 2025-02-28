/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.assistent;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.assistent.model.CursorInfo;
import com.e1c.edt.ai.assistent.model.IssueType;

public interface IFeedbackService
{
    CompletableFuture<Void> acceptedCodeAsync(String uuid, String code, Optional<CursorInfo> cursorStartInfo,
        Optional<CursorInfo> cursorEndInfo);

    CompletableFuture<Void> finalizeCodeAsync(String uuid, String code);

    CompletableFuture<Void> issueAsync(String uuid, IssueType type, String description);
}
