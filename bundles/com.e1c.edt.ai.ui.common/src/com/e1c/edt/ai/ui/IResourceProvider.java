/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;
import java.util.Set;

public interface IResourceProvider
{
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
