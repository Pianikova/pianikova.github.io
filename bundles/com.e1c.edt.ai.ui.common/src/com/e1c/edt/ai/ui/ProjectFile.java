/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Objects;

import org.eclipse.core.resources.IFile;

import com.e1c.edt.ai.AIContext;
import com.google.common.base.Preconditions;

public class ProjectFile
{
    public static final Comparator<ProjectFile> COMPARATOR =
        Comparator.comparing(file -> file.updateTime != null ? file.updateTime : LocalDateTime.MIN);
    public final String path;
    public final AIContext aiCtx;
    public final IFile file;
    private volatile LocalDateTime updateTime;
    private volatile String hash;
    private volatile long modificationStamp = -1;

    public ProjectFile(AIContext aiCtx, String path, IFile file, LocalDateTime updateTime)
    {
        Preconditions.checkNotNull(aiCtx);
        Preconditions.checkNotNull(path);
        Preconditions.checkNotNull(file);
        Preconditions.checkNotNull(updateTime);
        this.aiCtx = aiCtx;
        this.path = path;
        this.file = file;
        this.updateTime = updateTime;
    }

    public Duration getAge(LocalDateTime now)
    {
        return Duration.between(updateTime, now);
    }

    public void update(LocalDateTime updateTime, String hash, long modificationStamp)
    {
        this.updateTime = updateTime;
        this.hash = hash;
        this.modificationStamp = modificationStamp;
    }

    public long getModificationStamp()
    {
        return modificationStamp;
    }

    public String getHash()
    {
        return hash;
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(path);
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        ProjectFile other = (ProjectFile)obj;
        return Objects.equals(path, other.path);
    }
}
