/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.eclipse.jgit.lib.BranchTrackingStatus;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.storage.file.WindowCacheConfig;

import com.google.inject.Singleton;

/**
 * Common Git helper operations implementation
 */
@Singleton
public class JGitCommonHelper implements IJGitCommonHelper
{
    private static final AtomicBoolean WINDOW_CACHE_CONFIGURED = new AtomicBoolean(false);

    @Override
    public Repository openRepository(String workingDirectory) throws IOException
    {
        configureWindowCacheOnce();

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

    // On Windows, JGit's pack-file access via MappedByteBuffer (mmap) keeps OS file
    // handles alive until GC reclaims the buffer, which blocks deletion/rename of
    // .git/objects/pack/*.pack even after Repository.close(). Disabling mmap forces JGit
    // to use stream I/O whose handles are released synchronously on close.
    // POSIX (Linux/macOS) allows unlinking mmapped files, so we keep the default there.
    private static void configureWindowCacheOnce()
    {
        if (WINDOW_CACHE_CONFIGURED.compareAndSet(false, true))
        {
            if (isWindows())
            {
                var cfg = new WindowCacheConfig();
                cfg.setPackedGitMMAP(false);
                cfg.install();
            }
        }
    }

    /**
     * Force release of pack-file handles held by JGit's global WindowCache on Windows.
     * Re-installing a fresh WindowCacheConfig calls WindowCache.reconfigure, which
     * closes all previously cached PackFile instances synchronously. Call this after
     * each MCP-issued command so that file operations on .git/ that follow are not
     * blocked by stale handles. No-op on POSIX systems.
     */
    public static void releaseFileHandlesIfWindows()
    {
        if (isWindows())
        {
            new WindowCacheConfig().install();
        }
    }

    /**
     * Remove a stale {@code .git/index.lock} file that may have been left behind by
     * JGit's CloneCommand on Windows (known issue: DirCacheCheckout may not rename
     * the lock file before its handle is closed by the OS, leaving the lock file
     * on disk even after the operation reports success). Without this cleanup the
     * subsequent {@code switch}/{@code checkout}/{@code commit} fails with
     * "Cannot lock .git/index".
     *
     * @param workingDirectory the working tree directory to search for .git in;
     *     if null or no .git directory found, the method is a no-op.
     */
    public static void cleanupStaleIndexLock(String workingDirectory)
    {
        if (workingDirectory == null)
        {
            return;
        }
        try
        {
            var workDir = new File(workingDirectory);
            // The clone target lives one level below workingDirectory (the new
            // repository directory is created by clone). We don't know its name
            // here, so we look for .git both in workingDirectory and in its
            // immediate subdirectories (depth=1) — cheap and safe.
            removeIndexLockIn(workDir);
            var children = workDir.listFiles(File::isDirectory);
            if (children != null)
            {
                for (var child : children)
                {
                    removeIndexLockIn(child);
                }
            }
        }
        catch (Exception ignored)
        {
            // best-effort cleanup; never block command result on this
        }
    }

    private static void removeIndexLockIn(File dir)
    {
        var gitDir = new File(dir, ".git"); //$NON-NLS-1$
        if (!gitDir.isDirectory())
        {
            return;
        }
        deleteWithRetry(new File(gitDir, "index.lock")); //$NON-NLS-1$
        deleteWithRetry(new File(gitDir, "config.lock")); //$NON-NLS-1$
        deleteWithRetry(new File(gitDir, "HEAD.lock")); //$NON-NLS-1$
        var refs = new File(gitDir, "refs"); //$NON-NLS-1$
        deleteLocksUnder(refs);
    }

    private static void deleteLocksUnder(File dir)
    {
        if (!dir.isDirectory())
        {
            return;
        }
        var children = dir.listFiles();
        if (children == null)
        {
            return;
        }
        for (var c : children)
        {
            if (c.isDirectory())
            {
                deleteLocksUnder(c);
            }
            else if (c.getName().endsWith(".lock")) //$NON-NLS-1$
            {
                deleteWithRetry(c);
            }
        }
    }

    // On NTFS a deleted file may linger in "close-pending" state for a few
    // milliseconds (antivirus scanning, oplocks, handle release latency), during
    // which a subsequent createNewFile with the same name fails with ACCESS_DENIED.
    // We retry a few times with short backoff to give the OS time to finalize.
    private static void deleteWithRetry(File f)
    {
        if (!f.isFile())
        {
            return;
        }
        for (int i = 0; i < 10; i++)
        {
            if (f.delete() && !f.exists())
            {
                return;
            }
            try
            {
                Thread.sleep(20L);
            }
            catch (InterruptedException ie)
            {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    public static boolean isWindowsOs()
    {
        return isWindows();
    }

    private static boolean isWindows()
    {
        return System.getProperty("os.name", "").toLowerCase().contains("win"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
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
