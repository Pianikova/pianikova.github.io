/**
 *
 */
package com.e1c.edt.ai.ui;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.google.common.base.Preconditions;

/**
 * Represents information about a Git commit.
 */
public class GitCommitInfo
{
    private final String hash;
    private final String shortHash;
    private final String authorName;
    private final String authorEmail;
    private final long commitTime;
    private final String message;
    private final List<String> changedFiles;

    public GitCommitInfo(String hash, String shortHash, String authorName, String authorEmail,
        long commitTime, String message, List<String> changedFiles)
    {
        Preconditions.checkNotNull(hash);
        Preconditions.checkNotNull(shortHash);
        Preconditions.checkNotNull(authorName);
        Preconditions.checkNotNull(authorEmail);
        Preconditions.checkNotNull(message);
        Preconditions.checkNotNull(changedFiles);
        this.hash = hash;
        this.shortHash = shortHash;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.commitTime = commitTime;
        this.message = message;
        this.changedFiles = changedFiles;
    }

    /**
     * Full commit hash.
     */
    public String getHash()
    {
        return hash;
    }

    /**
     * Short commit hash (typically first 7-8 characters).
     */
    public String getShortHash()
    {
        return shortHash;
    }

    /**
     * Author name.
     */
    public String getAuthorName()
    {
        return authorName;
    }

    /**
     * Author email.
     */
    public String getAuthorEmail()
    {
        return authorEmail;
    }

    /**
     * Commit timestamp in epoch milliseconds.
     */
    public long getCommitTime()
    {
        return commitTime;
    }

    /**
     * Formatted commit time (ISO format).
     */
    public String getFormattedTime()
    {
        return Instant.ofEpochMilli(commitTime)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    }

    /**
     * Commit message.
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * List of changed files in this commit.
     */
    public List<String> getChangedFiles()
    {
        return changedFiles;
    }

    /**
     * Number of changed files.
     */
    public int getChangedFilesCount()
    {
        return changedFiles.size();
    }
}