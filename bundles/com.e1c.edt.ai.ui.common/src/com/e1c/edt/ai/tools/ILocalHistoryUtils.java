/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.util.List;

import org.eclipse.core.resources.IFile;

interface ILocalHistoryUtils
{
	List<LocalHistoryEntry> getLocalHistory(IFile file, int maxEntries) throws Exception;
}
