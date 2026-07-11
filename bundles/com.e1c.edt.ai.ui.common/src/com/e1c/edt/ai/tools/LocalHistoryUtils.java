/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IFileDocument;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class LocalHistoryUtils
	implements ILocalHistoryUtils
{
	private final IContentSourceProvider contentSourceProvider;

	@Inject
	LocalHistoryUtils(IContentSourceProvider contentSourceProvider)
	{
		Preconditions.checkNotNull(contentSourceProvider);

		this.contentSourceProvider = contentSourceProvider;
	}

	@Override
	public List<LocalHistoryEntry> getLocalHistory(IFile file, int maxEntries) throws Exception
	{
		var entries = new ArrayList<LocalHistoryEntry>();

		var location = file.getLocation();
		var current = getCurrentContent(file);

		var currentEntry = new LocalHistoryEntry();
		currentEntry.revisionId = "current"; //$NON-NLS-1$
		currentEntry.timestamp = file.getLocalTimeStamp();
		if (location != null && location.toFile().exists())
		{
			var currentAttrs = Files.readAttributes(location.toFile().toPath(), BasicFileAttributes.class);
			currentEntry.timestamp = currentAttrs.lastModifiedTime().toMillis();
		}
		currentEntry.formattedTime = formatTimestamp(currentEntry.timestamp);
		currentEntry.fileSize = current.content.length;
		currentEntry.location = location != null ? location.toFile().getAbsolutePath() : file.getFullPath().toString();
		currentEntry.isCurrent = true;
		currentEntry.isDirty = current.isDirty;
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

	@Override
	public CurrentFileContent getCurrentContent(IFile file) throws Exception
	{
		var fileDocument = contentSourceProvider.getFileDocument(file);
		if (fileDocument.isPresent() && fileDocument.get().isDirty())
		{
			return new CurrentFileContent(encode(fileDocument.get()), true);
		}

		// Prefer the exact on-disk bytes for a clean file: re-encoding the document may not
		// round-trip byte-for-byte (BOM, line endings normalized by the decoder).
		var location = file.getLocation();
		if (location != null && location.toFile().exists())
		{
			return new CurrentFileContent(Files.readAllBytes(location.toFile().toPath()), false);
		}

		if (fileDocument.isPresent())
		{
			return new CurrentFileContent(encode(fileDocument.get()), false);
		}

		throw new IllegalStateException("The file \"" + file.getFullPath() + "\" is not accessible."); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Override
	public Optional<Boolean> currentDiffersFromLatest(IFile file)
	{
		try
		{
			var historyStates = getHistoryStates(file);
			historyStates.sort(Comparator.comparingLong(IFileState::getModificationTime).reversed());

			for (var state : historyStates)
			{
				if (!state.exists())
				{
					continue;
				}

				var current = getCurrentContent(file);
				try (InputStream stream = state.getContents())
				{
					return Optional.of(!Arrays.equals(current.content, stream.readAllBytes()));
				}
			}

			return Optional.empty();
		}
		catch (Exception e)
		{
			return Optional.empty();
		}
	}

	@Override
	public Optional<String> getLatestRevisionId(IFile file)
	{
		try
		{
			var historyStates = getHistoryStates(file);
			historyStates.sort(Comparator.comparingLong(IFileState::getModificationTime).reversed());
			for (var state : historyStates)
			{
				if (state.exists())
				{
					return Optional.of(buildRevisionId(state));
				}
			}
			return Optional.empty();
		}
		catch (Exception e)
		{
			return Optional.empty();
		}
	}

	private static byte[] encode(IFileDocument fileDocument)
	{
		return fileDocument.getDocument().get().getBytes(fileDocument.getCharset());
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
