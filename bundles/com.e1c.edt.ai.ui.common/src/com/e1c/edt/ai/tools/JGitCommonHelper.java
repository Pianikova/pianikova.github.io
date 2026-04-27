/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.io.IOException;

import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import com.google.inject.Singleton;

/**
 * Common Git helper operations implementation
 */
@Singleton
public class JGitCommonHelper implements IJGitCommonHelper
{
    @Override
    public Repository openRepository(String workingDirectory) throws IOException
    {
        var workDir = new File(workingDirectory);
        var gitDir = findGitDirectory(workDir);

        if (gitDir == null)
        {
            return null;
        }

        // Set gitDir and workTree explicitly.
        // Do NOT call readEnvironment() — it can override gitDir/workTree via GIT_DIR /
        // GIT_INDEX_FILE env-vars, causing index writes to go to an unrelated location.
        // Do NOT call findGitDir() — it is a no-op when gitDir is already set, but
        // calling it resets the work-tree to the JVM working directory on some JGit versions.
        var builder = new FileRepositoryBuilder();
        builder.setGitDir(gitDir);
        builder.setWorkTree(gitDir.getParentFile());
        return builder.build();
    }

    @SuppressWarnings("nls")
    @Override
    public File findGitDirectory(File directory)
    {
        var gitDir = new File(directory, ".git");
        if (gitDir.exists() && gitDir.isDirectory())
        {
            return gitDir;
        }

        var parentDir = directory.getParentFile();
        if (parentDir != null)
        {
            gitDir = new File(parentDir, ".git");
            if (gitDir.exists() && gitDir.isDirectory())
            {
                return gitDir;
            }
        }

        return null;
    }

    @Override
    public BranchTrackingStatus getBranchTrackingStatus(org.eclipse.jgit.api.Git git, String branchName)
    {
        try
        {
            return BranchTrackingStatus.of(git.getRepository(), branchName);
        }
        catch (Exception e)
        {
            return null;
        }
    }

    @Override
    public org.eclipse.jgit.treewalk.AbstractTreeIterator prepareTreeParser(Repository repository, RevCommit commit)
        throws IOException
    {
        try (var reader = repository.newObjectReader())
        {
            var tree = commit.getTree();
            var treeParser = new org.eclipse.jgit.treewalk.CanonicalTreeParser();
            treeParser.reset(reader, tree);
            return treeParser;
        }
    }
}
