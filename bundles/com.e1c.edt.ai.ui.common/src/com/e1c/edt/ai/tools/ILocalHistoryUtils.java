/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IFile;

public interface ILocalHistoryUtils
{
	List<LocalHistoryEntry> getLocalHistory(IFile file, int maxEntries) throws Exception;

	/**
	 * Returns the current content of the file: the live editor buffer when it has unsaved
	 * changes, otherwise the on-disk content.
	 */
	CurrentFileContent getCurrentContent(IFile file) throws Exception;

	/**
	 * Tells whether the current content differs from the newest local history revision.
	 *
	 * @return empty when the file has no local history or the comparison failed
	 */
	Optional<Boolean> currentDiffersFromLatest(IFile file);

	/**
	 * Returns the revision id of the newest local history revision of the file.
	 *
	 * @return empty when the file has no local history or it could not be read
	 */
	Optional<String> getLatestRevisionId(IFile file);
}
