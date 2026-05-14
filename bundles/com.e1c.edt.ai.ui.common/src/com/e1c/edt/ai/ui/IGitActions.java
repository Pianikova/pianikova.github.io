/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import com.e1c.edt.ai.ICancellationToken;

public interface IGitActions
{
    void reviewGitChanges(List<GitDiff> diffs, ICancellationToken cancellationToken);

    CompletableFuture<Optional<String>> feedbackAsync(CommitMessage commitMessage, String finalText,
        ICancellationToken cancellationToken);
}
