/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

public interface IFileSystem
{
    Optional<String> getText(IFileContent contentReader, int firstLineNumber, int linesNumber);
}
