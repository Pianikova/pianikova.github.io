/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.List;

import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IObservable;

public interface IGitActions
{
    IObservable<String> ceateGitCommitMessageSource(String baseCommitMessage, List<GitDiff> diffs,
        ICancellationToken cancellationToken);
}
