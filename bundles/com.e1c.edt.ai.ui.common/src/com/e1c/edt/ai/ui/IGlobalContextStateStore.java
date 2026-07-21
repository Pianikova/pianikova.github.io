/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Map;

import org.eclipse.core.resources.IProject;

/**
 * Persists the per-project global-context sync state (<code>path -&gt; {localTimeStamp, hash}</code>) so that on the
 * next EDT start the tracking workflow can seed its in-memory maps and skip re-hashing / re-syncing files that have
 * not changed since the previous session.
 */
public interface IGlobalContextStateStore
{
    /**
     * Loads the persisted state for the given project. Returns an empty (mutable) map when no state has been stored
     * yet or the stored file could not be read/parsed. Never throws.
     */
    Map<String, GlobalContextFileState> load(IProject project);

    /**
     * Persists the given state for the project, replacing any previously stored state. Never throws; failures are
     * logged.
     */
    void save(IProject project, Map<String, GlobalContextFileState> state);

    /**
     * Removes the persisted state for the project (e.g. when the project is deleted from the workspace). Never
     * throws; failures are logged.
     */
    void delete(IProject project);
}
