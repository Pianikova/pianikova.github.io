/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;

/**
 * Interface for common Git helper operations
 */
public interface IJGitCommonHelper
{
    /**
     * Open a Git repository from the working directory
     * @param workingDirectory working directory path
     * @return repository or null if not found
     * @throws IOException if repository cannot be opened
     */
    Repository openRepository(String workingDirectory) throws IOException;

    /**
     * Find .git directory starting from the given directory
     * @param directory directory to start searching from
     * @return .git directory or null if not found
     */
    File findGitDirectory(File directory);

    /**
     * Get branch tracking status
     * @param git Git instance
     * @param branchName branch name
     * @return tracking status or null if not available
     */
    BranchTrackingStatus getBranchTrackingStatus(org.eclipse.jgit.api.Git git, String branchName);

    /**
     * Prepare tree parser for a commit
     * @param repository Git repository
     * @param commit commit to parse
     * @return tree iterator
     * @throws IOException if parsing fails
     */
    org.eclipse.jgit.treewalk.AbstractTreeIterator prepareTreeParser(Repository repository, RevCommit commit)
        throws IOException;
}
