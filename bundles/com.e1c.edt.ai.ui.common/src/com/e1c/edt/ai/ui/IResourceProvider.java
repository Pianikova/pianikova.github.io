/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

public interface IResourceProvider
{
    public static final String PROMTS_GIT_COMMIT = "prompts/git_commit.txt"; //$NON-NLS-1$
    public static final String SUGGEST_YOU_OPTION = "prompts/suggest_your_option.txt"; //$NON-NLS-1$
    public static final String CORRECT_ERRORS = "prompts/correct_errors.txt"; //$NON-NLS-1$
    public static final String IN_OTHER_WORDS = "prompts/in_other_words.txt"; //$NON-NLS-1$
    public static final String IMPROVE_STYLE = "prompts/improve_style.txt"; //$NON-NLS-1$

    Optional<String> getTextResource(String filePath);
}
