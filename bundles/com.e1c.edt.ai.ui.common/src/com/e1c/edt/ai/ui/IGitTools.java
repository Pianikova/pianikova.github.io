/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

public interface IGitTools
{
    void getDiff(Repository repository, int contextLines, OutputStream gitDiffStream)
        throws GitAPIException, IOException;
}
