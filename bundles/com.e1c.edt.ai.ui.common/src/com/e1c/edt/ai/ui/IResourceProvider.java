/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;
import java.util.Set;

public interface IResourceProvider
{
    public static final String SUGGEST_YOUR_OPTION = "prompts/suggest_your_option.txt"; //$NON-NLS-1$
    public static final String CORRECT_ERRORS = "prompts/correct_errors.txt"; //$NON-NLS-1$
    public static final String IN_OTHER_WORDS = "prompts/in_other_words.txt"; //$NON-NLS-1$
    public static final String IMPROVE_STYLE = "prompts/improve_style.txt"; //$NON-NLS-1$

    Optional<String> getTextResource(String filePath);

    /**
     * Lists the immediate child names (files and directories) under the given resource directory,
     * looking in both the external resources directory and the plugin bundle.
     *
     * @param dirPath the bundle-relative directory path, e.g. {@code skills}.
     * @return the set of immediate child names, never {@code null}.
     */
    Set<String> listChildNames(String dirPath);
}
