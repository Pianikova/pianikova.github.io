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

        var builder = new FileRepositoryBuilder();
        builder.setGitDir(gitDir);
        builder.readEnvironment();
        builder.findGitDir();
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
