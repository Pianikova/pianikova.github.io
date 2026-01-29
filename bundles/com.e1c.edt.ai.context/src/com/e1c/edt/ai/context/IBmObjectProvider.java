/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.Optional;

import org.eclipse.core.resources.IFile;

import com._1c.g5.v8.bm.core.IBmObject;

/**
 * Interface for getting BM objects from files
 */
public interface IBmObjectProvider
{
    /**
     * Gets BM object from file
     * @param file the file to get BM object from
     * @return Optional containing BM object or empty if not found
     */
    Optional<IBmObject> getObject(IFile file);
}