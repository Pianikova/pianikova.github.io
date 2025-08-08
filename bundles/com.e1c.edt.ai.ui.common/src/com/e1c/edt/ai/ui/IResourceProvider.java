/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

public interface IResourceProvider
{
    public static final String PROMTS_GIT_COMMIT = "prompts/git_commit.txt"; //$NON-NLS-1$

    Optional<String> getTextResource(String filePath);
}
