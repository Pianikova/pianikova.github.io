/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;

public interface IGitTools
{
    void getDiff(Repository repository, int contextLines, OutputStream gitDiffStream)
        throws GitAPIException, IOException;
    
    String getDiffText(Repository repository, int contextLines)
        throws GitAPIException, IOException;

    String getUncommittedDiffText(Repository repository, int contextLines)
        throws GitAPIException, IOException;
    
    String getDiffText(Repository repository, String oldCommit, String newCommit, int contextLines)
        throws GitAPIException, IOException;
    
    List<GitCommitInfo> getCommitHistory(Repository repository, int maxCommits)
        throws GitAPIException, IOException;
}
