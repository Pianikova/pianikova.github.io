/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;

class LocalHistoryUtils
	implements ILocalHistoryUtils
{
	@Override
	public List<LocalHistoryEntry> getLocalHistory(IFile file, int maxEntries) throws Exception
	{
		var entries = new ArrayList<LocalHistoryEntry>();

		var absolutePath = file.getLocation().toFile().getAbsolutePath();
		var filePath = Paths.get(absolutePath);

		var currentEntry = new LocalHistoryEntry();
		var currentAttrs = Files.readAttributes(filePath, BasicFileAttributes.class);
		currentEntry.revisionId = "current"; //$NON-NLS-1$
		currentEntry.timestamp = currentAttrs.lastModifiedTime().toMillis();
		currentEntry.formattedTime = formatTimestamp(currentEntry.timestamp);
		currentEntry.fileSize = currentAttrs.size();
		currentEntry.location = absolutePath;
		currentEntry.isCurrent = true;
		entries.add(currentEntry);

		if (maxEntries <= 1)
		{
			return entries;
		}

		var historyStates = getHistoryStates(file);
		historyStates.sort(Comparator.comparingLong(IFileState::getModificationTime).reversed());

		var limit = Math.min(maxEntries - 1, historyStates.size());
		for (int i = 0; i < limit; i++)
		{
			if (entries.size() >= maxEntries)
			{
				break;
			}

			var state = historyStates.get(i);
			if (!state.exists())
			{
				continue;
			}

			var timestamp = state.getModificationTime();
			var entry = new LocalHistoryEntry();
			entry.revisionId = buildRevisionId(state);
			entry.timestamp = timestamp;
			entry.formattedTime = formatTimestamp(timestamp);
			entry.fileSize = readStateSize(state);
			entry.location = "local_history:" + entry.revisionId; //$NON-NLS-1$
			entry.isCurrent = false;

			entries.add(entry);
		}

		return entries;
	}

	private static List<IFileState> getHistoryStates(IFile file) throws CoreException
	{
		var states = file.getHistory(null);
		if (states == null || states.length == 0)
		{
			return new ArrayList<>();
		}
		return new ArrayList<>(Arrays.asList(states));
	}

	private static String buildRevisionId(IFileState state)
	{
		return state.getName() + "_" + generateRevisionId(state.getModificationTime()); //$NON-NLS-1$
	}

	private static long readStateSize(IFileState state)
	{
		try (InputStream stream = state.getContents())
		{
			long total = 0;
			var buffer = new byte[8192];
			int read;
			while ((read = stream.read(buffer)) >= 0)
			{
				total += read;
			}
			return total;
		}
		catch (Exception e)
		{
			return -1;
		}
	}

	private static String generateRevisionId(long timestamp)
	{
		return DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss") //$NON-NLS-1$
			.format(Instant.ofEpochMilli(timestamp).atZone(ZoneId.systemDefault()));
	}

	private static String formatTimestamp(long timestamp)
	{
		return Instant.ofEpochMilli(timestamp)
			.atZone(ZoneId.systemDefault())
			.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
	}
}
