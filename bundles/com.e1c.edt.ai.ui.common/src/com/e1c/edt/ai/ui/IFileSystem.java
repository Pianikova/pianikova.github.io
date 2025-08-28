/**
 *
 */
package com.e1c.edt.ai.ui;

import java.nio.file.Path;
import java.util.Optional;

public interface IFileSystem
{
    Optional<String> getText(Path filePath);

    boolean isTextFile(Path filePath);
}
