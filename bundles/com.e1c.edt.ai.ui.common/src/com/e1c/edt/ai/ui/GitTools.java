/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.diff.DiffEntry;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.lib.Repository;

public class GitTools implements IGitTools
{
    @Override
    public void getDiff(Repository repository, int contextLines, OutputStream gitDiffStream)
        throws GitAPIException, IOException
    {
        try (var git = new Git(repository); var diffFormatter = new DiffFormatter(gitDiffStream))
        {
            diffFormatter.setRepository(repository);
            diffFormatter.setContext(contextLines);
            var diffs = git.diff().setCached(true).setShowNameAndStatusOnly(false).call();
            for (DiffEntry entry : diffs)
            {
                diffFormatter.format(entry);
            }
        }
    }
}
