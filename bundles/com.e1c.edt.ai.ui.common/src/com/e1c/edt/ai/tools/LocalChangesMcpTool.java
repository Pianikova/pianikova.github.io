/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jgit.diff.DiffAlgorithm;
import org.eclipse.jgit.diff.DiffFormatter;
import org.eclipse.jgit.diff.RawText;
import org.eclipse.jgit.diff.RawTextComparator;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.IProjectTools;
import com.e1c.edt.ai.TextColor;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class LocalChangesMcpTool
	implements IMcpTool
{
	public static final String TOOL_NAME = "LocalChanges"; //$NON-NLS-1$
	private static final String CURRENT_REVISION = "current"; //$NON-NLS-1$
	private static final String LATEST_REVISION = "latest"; //$NON-NLS-1$
	private static final String OLDEST_REVISION = "oldest"; //$NON-NLS-1$
	private static final String PREVIOUS_REVISION = "previous"; //$NON-NLS-1$
	private static final String LOCAL_HISTORY_PREFIX = "local_history:"; //$NON-NLS-1$
	private static final int DEFAULT_CONTEXT_LINES = McpToolConstants.DEFAULT_GIT_DIFF_CONTEXT_LINES;
	private static final int DEFAULT_MAX_ENTRIES = McpToolConstants.DEFAULT_MAX_HISTORY_ENTRIES;

	// @formatter:off
	@SuppressWarnings("nls")
	private static String QuestionExample =
		"{\n"
		+ "  \"project_name\": \"MyProject\",\n"
		+ "  \"file_path\": \"src/com/example/MyClass.java\",\n"
		+ "  \"revision_id\": \"file_20240101-120000\",\n"
		+ "  \"context_lines\": 3\n"
		+ "}";

	@SuppressWarnings("nls")
	private static String QuestionExampleWithLocation =
		"{\n"
		+ "  \"project_name\": \"MyProject\",\n"
		+ "  \"file_path\": \"src/com/example/MyClass.java\",\n"
		+ "  \"history_location\": \"C:/workspace/.metadata/.plugins/org.eclipse.core.resources/.history/123abc\",\n"
		+ "  \"context_lines\": 3\n"
		+ "}";

	@SuppressWarnings("nls")
	private static String QuestionExampleBetweenRevisions =
		"{\n"
		+ "  \"project_name\": \"MyProject\",\n"
		+ "  \"file_path\": \"src/com/example/MyClass.java\",\n"
		+ "  \"from_revision_id\": \"oldest\",\n"
		+ "  \"to_revision_id\": \"latest\",\n"
		+ "  \"context_lines\": 3\n"
		+ "}";

	@SuppressWarnings("nls")
	private static String QuestionExampleFirstChange =
		"{\n"
		+ "  \"project_name\": \"MyProject\",\n"
		+ "  \"file_path\": \"src/com/example/MyClass.java\",\n"
		+ "  \"from_index\": 12,\n"
		+ "  \"to_index\": 11,\n"
		+ "  \"context_lines\": 3,\n"
		+ "  \"max_entries\": 0\n"
		+ "}";

	@SuppressWarnings("nls")
	private static String AnswerExample =
		"{\n"
		+ "  \"project_name\": \"MyProject\",\n"
		+ "  \"file_path\": \"src/com/example/MyClass.java\",\n"
		+ "  \"from_revision_id\": \"file_20240101-120000\",\n"
		+ "  \"from_history_location\": \"C:/workspace/.metadata/.plugins/org.eclipse.core.resources/.history/123abc\",\n"
		+ "  \"to_revision_id\": \"current\",\n"
		+ "  \"to_history_location\": \"C:/workspace/MyProject/src/com/example/MyClass.java\",\n"
		+ "  \"diff_text\": \"diff --git a/src/example.java b/src/example.java\\n--- a/src/example.java\\n+++ b/src/example.java\\n@@ -1,3 +1,3 @@\\n public class Example {\\n-    private int oldField;\\n+    private int newField;\\n }\",\n"
		+ "  \"context_lines\": 3,\n"
		+ "  \"has_changes\": true\n"
		+ "}";
	// @formatter:on

	private final IJson json;
	private final McpToolCallSpecification spec;
	private final IMcpToolsCallMessageFactory messageFactory;
	private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
	private final IMarkdownUtils markdownUtils;
	private final ILocalHistoryUtils localHistoryUtils;
	private final IProjectTools projectTools;

	@Inject
	public LocalChangesMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
		Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IMarkdownUtils markdownUtils,
        ILocalHistoryUtils localHistoryUtils, IProjectTools projectTools)
	{
		Preconditions.checkNotNull(json);
		Preconditions.checkNotNull(messageFactory);
		Preconditions.checkNotNull(cancellationProgressMonitor);
		Preconditions.checkNotNull(markdownUtils);
		Preconditions.checkNotNull(localHistoryUtils);
		Preconditions.checkNotNull(projectTools);

		this.json = json;
		this.messageFactory = messageFactory;
		this.cancellationProgressMonitor = cancellationProgressMonitor;
		this.markdownUtils = markdownUtils;
		this.localHistoryUtils = localHistoryUtils;
		this.projectTools = projectTools;

		spec = createSpecification();
	}

	@Override
	public McpToolCallSpecification getSpecification()
	{
		return spec;
	}

	@SuppressWarnings({ "nls" })
	@Override
	public CompletableFuture<ToolCallMessage> call(McpToolCall call, ICancellationToken cancellationToken)
	{
		var details = new ToolCallMessageDetails();
        details.autoCall = true;

		var optionalRequest = json.deserialize(call.function.arguments, Request.class);
		if (optionalRequest.isEmpty())
		{
			throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
		}

		var request = optionalRequest.get();
		var projectName = request.projectName;
		if (projectName == null || projectName.isBlank())
		{
			throw new ToolException("`project_name` is required.");
		}

		var filePath = request.filePath;
		if (filePath == null || filePath.isBlank())
		{
			throw new ToolException("`file_path` is required.");
		}

		var contextLines = request.contextLines != null && request.contextLines > 0 ? request.contextLines : DEFAULT_CONTEXT_LINES;
		int maxEntries;
		if (request.maxEntries == null)
		{
			maxEntries = DEFAULT_MAX_ENTRIES;
		}
		else if (request.maxEntries <= 0)
		{
			maxEntries = Integer.MAX_VALUE;
		}
		else
		{
			maxEntries = request.maxEntries;
		}

		var hasFromSelectors = hasAny(request.fromRevisionId, request.fromHistoryLocation, request.fromIndex);
		var hasToSelectors = hasAny(request.toRevisionId, request.toHistoryLocation, request.toIndex);
		var hasLegacySelectors = hasAny(request.revisionId, request.historyLocation);

		if (!hasFromSelectors && !hasToSelectors && !hasLegacySelectors)
		{
			throw new ToolException(
				"`revision_id` or `history_location` is required, or provide `from_*`/`to_*` selectors.");
		}

		if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = MessageFormat.format(Messages.LocalChangesTitleTemplate,
                projectName != null ? projectName : Messages.CurrentProject, filePath != null ? filePath : Messages.SelectedFile);
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

		return CompletableFuture.supplyAsync(() ->
		{
			if (cancellationToken.isCanceled())
			{
				throw new ToolException("Operation was cancelled before execution.");
			}

			var root = ResourcesPlugin.getWorkspace().getRoot();
			var project = root.getProject(projectName);

			if (project == null || !project.exists())
			{
				throw new ToolException("The project \"" + projectName + "\" does not exist.");
			}
			if (!project.isOpen())
			{
				try
				{
					var monitor = cancellationProgressMonitor.get();
					monitor.setCancellationToken(cancellationToken);
					project.open(monitor);
				}
				catch (CoreException error)
				{
					throw new ToolException("Cannot open the project \"" + projectName + "\". " + error.getMessage(), error,
						ToolErrorType.RETRYABLE);
				}
			}

			var file = projectTools.getProjectFile(project, filePath);
			if (!file.isPresent())
			{
				throw new ToolException("The file \"" + filePath + "\" does not exist within the IDE project context. "
					+ "The file may exist outside the project directory, but IDE tools can only access files within the current project scope.");
			}

			var actualFile = file.get();

			try
			{
				var fromSelector = new RevisionSelector();
				var toSelector = new RevisionSelector();
				if (hasFromSelectors || hasToSelectors)
				{
					fromSelector.revisionId = request.fromRevisionId;
					fromSelector.historyLocation = request.fromHistoryLocation;
					fromSelector.index = request.fromIndex;

					toSelector.revisionId = request.toRevisionId;
					toSelector.historyLocation = request.toHistoryLocation;
					toSelector.index = request.toIndex;

					if (!hasFromSelectors)
					{
						fromSelector.revisionId = CURRENT_REVISION;
					}
					if (!hasToSelectors)
					{
						toSelector.revisionId = CURRENT_REVISION;
					}
				}
				else
				{
					fromSelector.revisionId = request.revisionId;
					fromSelector.historyLocation = request.historyLocation;
					toSelector.revisionId = CURRENT_REVISION;
				}

				var historyEntries =
					needsHistoryEntries(fromSelector) || needsHistoryEntries(toSelector)
						? localHistoryUtils.getLocalHistory(actualFile, maxEntries)
						: null;
				if (historyEntries != null)
				{
					var lastIndex = historyEntries.size() - 1;
					for (int i = 0; i < historyEntries.size(); i++)
					{
						var entry = historyEntries.get(i);
						entry.index = i;
						entry.isOldest = i == lastIndex;
					}
				}

				var historyStates =
					needsHistoryStates(fromSelector) || needsHistoryStates(toSelector)
						? getHistoryStates(actualFile, maxEntries)
						: null;

				var fromRevision = resolveRevision(actualFile, fromSelector, historyEntries, historyStates);
				var toRevision = resolveRevision(actualFile, toSelector, historyEntries, historyStates);

				var oldContent = fromRevision.getContent();
				var newContent = toRevision.getContent();
				var diffText = createDiffText(filePath, oldContent, newContent, contextLines);
				var hasChanges = !diffText.trim().isEmpty();

				var response = new LocalChangesResponse();
				response.projectName = projectName;
				response.filePath = filePath;
				response.fromRevisionId = fromRevision.revisionId;
				response.fromHistoryLocation = fromRevision.historyLocation;
				response.toRevisionId = toRevision.revisionId;
				response.toHistoryLocation = toRevision.historyLocation;
				response.diffText = diffText;
				response.contextLines = contextLines;
				response.hasChanges = hasChanges;

				var content = json.serialize(response);

				var responseMarkdown = new StringBuilder();
				var revisionLabel = markdownUtils.createStyledText(
                    response.fromRevisionId + " -> " + response.toRevisionId, TextColor.BLUE, FontWeight.NORMAL, false);

				if (hasChanges)
				{
					responseMarkdown.append(MessageFormat.format(Messages.LocalChangesFoundTemplate,
						markdownUtils.escapeForMarkdown(filePath),
						revisionLabel,
						markdownUtils.escapeForMarkdown(projectName)));

					responseMarkdown.append("\n\n");
					responseMarkdown.append(markdownUtils.buildUnifiedDiffByFile(diffText));
				}
				else
				{
					responseMarkdown.append(MessageFormat.format(Messages.NoLocalChangesFoundTemplate,
						markdownUtils.escapeForMarkdown(filePath),
						revisionLabel,
						markdownUtils.escapeForMarkdown(projectName)));
				}

				details.responseMarkdown = responseMarkdown.toString();
				return messageFactory.createMessage(this, call, content, details);
			}
			catch (Exception e)
			{
				throw new ToolException("Failed to get local changes: " + e.getMessage(), e, ToolErrorType.RETRYABLE);
			}
		});
	}

    @SuppressWarnings("nls")
    private static String createDiffText(String filePath, byte[] oldContent, byte[] newContent, int contextLines)
		throws Exception
	{
		var oldText = new RawText(oldContent);
		var newText = new RawText(newContent);
		var diffAlgorithm = DiffAlgorithm.getAlgorithm(DiffAlgorithm.SupportedAlgorithm.HISTOGRAM);
		var edits = diffAlgorithm.diff(RawTextComparator.DEFAULT, oldText, newText);

		if (edits.isEmpty())
		{
			return ""; //$NON-NLS-1$
		}

		var normalizedPath = filePath.replace('\\', '/');
		var output = new ByteArrayOutputStream();
        output
            .write(("diff --git a/" + normalizedPath + " b/" + normalizedPath + "\n").getBytes(StandardCharsets.UTF_8));
        output.write(("--- a/" + normalizedPath + "\n").getBytes(StandardCharsets.UTF_8));
        output.write(("+++ b/" + normalizedPath + "\n").getBytes(StandardCharsets.UTF_8));

		try (var formatter = new DiffFormatter(output))
		{
			formatter.setContext(contextLines);
			formatter.format(edits, oldText, newText);
		}

		return output.toString(StandardCharsets.UTF_8);
	}

	private static boolean hasAny(String value, String otherValue)
	{
		return (value != null && !value.isBlank()) || (otherValue != null && !otherValue.isBlank());
	}

	private static boolean hasAny(String value, String otherValue, Integer index)
	{
		return (value != null && !value.isBlank()) || (otherValue != null && !otherValue.isBlank()) || index != null;
	}

	private static boolean needsHistoryEntries(RevisionSelector selector)
	{
		if (selector == null)
		{
			return false;
		}
		if (selector.index != null)
		{
			return true;
		}
		if (selector.historyLocation != null && !selector.historyLocation.isBlank())
		{
			return false;
		}
		if (selector.revisionId == null || selector.revisionId.isBlank())
		{
			return false;
		}
		return !isCurrentRevision(selector.revisionId);
	}

	private static boolean needsHistoryStates(RevisionSelector selector)
	{
		if (selector == null)
		{
			return false;
		}
		if (selector.historyLocation != null && selector.historyLocation.startsWith(LOCAL_HISTORY_PREFIX))
		{
			return true;
		}
		if (selector.index != null)
		{
			return true;
		}
		if (selector.revisionId == null || selector.revisionId.isBlank())
		{
			return false;
		}
		return !isCurrentRevision(selector.revisionId);
	}

	private static boolean isCurrentRevision(String revisionId)
	{
		return CURRENT_REVISION.equalsIgnoreCase(revisionId);
	}

	private static boolean isLatestRevision(String revisionId)
	{
		return LATEST_REVISION.equalsIgnoreCase(revisionId) || PREVIOUS_REVISION.equalsIgnoreCase(revisionId);
	}

	private static boolean isOldestRevision(String revisionId)
	{
		return OLDEST_REVISION.equalsIgnoreCase(revisionId);
	}

    @SuppressWarnings("nls")
    private static ResolvedRevision resolveRevision(org.eclipse.core.resources.IFile file, RevisionSelector selector,
		Iterable<LocalHistoryEntry> historyEntries, List<HistoryState> historyStates)
	{
		if (selector == null)
		{
			throw new RuntimeException("Revision selector is not specified.");
		}

		if (selector.historyLocation != null && !selector.historyLocation.isBlank())
		{
			if (selector.historyLocation.startsWith(LOCAL_HISTORY_PREFIX))
			{
				var revisionId = selector.historyLocation.substring(LOCAL_HISTORY_PREFIX.length());
				if (revisionId.isBlank())
				{
					revisionId = selector.revisionId;
				}
				return resolveHistoryState(revisionId, historyStates);
			}

			var path = Paths.get(selector.historyLocation);
			var revisionId = selector.revisionId != null && !selector.revisionId.isBlank()
				? selector.revisionId
				: path.getFileName().toString();
			return ResolvedRevision.forPath(path, revisionId, selector.historyLocation);
		}

		if (selector.index != null)
		{
			var entry = getEntryByIndex(historyEntries, selector.index);
			if (entry.isCurrent)
			{
				return resolveCurrent(file);
			}
			return resolveHistoryState(entry.revisionId, historyStates);
		}

		if (selector.revisionId != null && !selector.revisionId.isBlank())
		{
			if (isCurrentRevision(selector.revisionId))
			{
				return resolveCurrent(file);
			}
			if (isLatestRevision(selector.revisionId))
			{
				var entry = getLatestHistoryEntry(historyEntries, historyStates);
				return resolveHistoryState(entry.revisionId, historyStates);
			}
			if (isOldestRevision(selector.revisionId))
			{
				var entry = getOldestHistoryEntry(historyEntries, historyStates);
				return resolveHistoryState(entry.revisionId, historyStates);
			}
			return resolveHistoryState(selector.revisionId, historyStates);
		}

		return resolveCurrent(file);
	}

	private static ResolvedRevision resolveCurrent(org.eclipse.core.resources.IFile file)
	{
		var path = Paths.get(file.getLocation().toFile().getAbsolutePath());
		return ResolvedRevision.forPath(path, CURRENT_REVISION, path.toString());
	}

    @SuppressWarnings("nls")
    private static LocalHistoryEntry getEntryByIndex(Iterable<LocalHistoryEntry> historyEntries, int index)
	{
		if (historyEntries == null)
		{
			throw new RuntimeException("Local history is required to resolve index.");
		}
		for (var entry : historyEntries)
		{
			if (entry.index != null && entry.index == index)
			{
				return entry;
			}
		}
		throw new RuntimeException("Local history entry with index \"" + index + "\" was not found.");
	}

    @SuppressWarnings("nls")
    private static LocalHistoryEntry getLatestHistoryEntry(Iterable<LocalHistoryEntry> historyEntries,
		List<HistoryState> historyStates)
	{
		if (historyEntries == null)
		{
			throw new RuntimeException("Local history is required to resolve latest revision.");
		}
		for (var entry : historyEntries)
		{
			if (!entry.isCurrent)
			{
				return entry;
			}
		}
		if (historyStates != null && !historyStates.isEmpty())
		{
			var first = historyStates.get(0);
			var entry = new LocalHistoryEntry();
			entry.revisionId = first.revisionId;
			entry.isCurrent = false;
			return entry;
		}
		throw new RuntimeException("No local history entries available.");
	}

    @SuppressWarnings("nls")
    private static LocalHistoryEntry getOldestHistoryEntry(Iterable<LocalHistoryEntry> historyEntries,
		List<HistoryState> historyStates)
	{
		if (historyEntries == null)
		{
			throw new RuntimeException("Local history is required to resolve oldest revision.");
		}
		LocalHistoryEntry oldest = null;
		for (var entry : historyEntries)
		{
			if (!entry.isCurrent)
			{
				oldest = entry;
			}
		}
		if (oldest == null)
		{
			if (historyStates != null && !historyStates.isEmpty())
			{
				var entry = new LocalHistoryEntry();
				entry.revisionId = historyStates.get(historyStates.size() - 1).revisionId;
				entry.isCurrent = false;
				return entry;
			}
			throw new RuntimeException("No local history entries available.");
		}
		return oldest;
	}

    @SuppressWarnings("nls")
    private static ResolvedRevision resolveHistoryState(String revisionId, List<HistoryState> historyStates)
	{
		if (revisionId == null || revisionId.isBlank())
		{
			throw new RuntimeException("Revision id is required to resolve history state.");
		}
		if (historyStates == null)
		{
			throw new RuntimeException("Local history is required to resolve revision \"" + revisionId + "\".");
		}
		for (var state : historyStates)
		{
			if (revisionId.equals(state.revisionId))
			{
				return ResolvedRevision.forContent(state.content, revisionId, LOCAL_HISTORY_PREFIX + revisionId);
			}
		}
		throw new RuntimeException("Local history entry \"" + revisionId + "\" was not found.");
	}

	private static List<HistoryState> getHistoryStates(org.eclipse.core.resources.IFile file, int maxEntries)
		throws CoreException
	{
		var states = file.getHistory(null);
		var result = new ArrayList<HistoryState>();
		if (states == null || states.length == 0)
		{
			return result;
		}

		var historyStates = Arrays.asList(states);
		historyStates.sort((s1, s2) -> Long.compare(s2.getModificationTime(), s1.getModificationTime()));

		int limit = historyStates.size();
		if (maxEntries != Integer.MAX_VALUE)
		{
			limit = Math.min(maxEntries - 1, historyStates.size());
		}

		for (int i = 0; i < limit; i++)
		{
			var state = historyStates.get(i);
			if (!state.exists())
			{
				continue;
			}
			var historyState = new HistoryState();
			historyState.revisionId = buildRevisionId(state);
			historyState.content = readStateContent(state);
			result.add(historyState);
		}

		return result;
	}

	private static String buildRevisionId(IFileState state)
	{
		return state.getName() + "_" + generateRevisionId(state.getModificationTime()); //$NON-NLS-1$
	}

    @SuppressWarnings("nls")
    private static byte[] readStateContent(IFileState state)
	{
		try (InputStream stream = state.getContents())
		{
			return stream.readAllBytes();
		}
		catch (Exception e)
		{
			throw new RuntimeException("Failed to read local history content: " + e.getMessage(), e);
		}
	}

	private static String generateRevisionId(long timestamp)
	{
		return java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss") //$NON-NLS-1$
			.format(java.time.Instant.ofEpochMilli(timestamp).atZone(java.time.ZoneId.systemDefault()));
	}

	@SuppressWarnings("nls")
	private static McpToolCallSpecification createSpecification()
	{
		var spec = new McpToolCallSpecification();
		spec.type = "function";
		spec.function = new McpToolCallFunction();
		spec.function.name = TOOL_NAME;

		var description = new StringBuilder();
		description.append("Diffs local history revisions and returns a Git-style diff.");
		description.append("\n\nUsage:");
		description.append("\n- Arguments must be a single JSON object.");
		description.append("\n- Provide `from_*` and `to_*` selectors to diff two revisions.");
		description.append("\n- If only one side is provided, the other side defaults to `current`.");
		description.append("\n- `revision_id` supports special values: `current`, `latest`, `oldest`, `previous`.");
		description.append("\n- Use `from_index`/`to_index` with `LocalHistory` indexes to get first changes.");
		description.append("\n- Returns diff in standard Git diff format.");
		description.append("\n\nRelated tools:");
		description.append("\n- List revisions: `" + LocalHistoryMcpTool.TOOL_NAME + "`.");
		description.append("\n\nLocal history revision diff example:");
		description.append("\n  Q: "); description.append(QuestionExample);
		description.append("\n  A: "); description.append(AnswerExample);
		description.append("\n\nBetween revisions example:");
		description.append("\n  Q: "); description.append(QuestionExampleBetweenRevisions);
		description.append("\n  A: "); description.append(AnswerExample);
		description.append("\n\nFirst change example (oldest -> next newer):");
		description.append("\n  Q: "); description.append(QuestionExampleFirstChange);
		description.append("\n  A: "); description.append(AnswerExample);
		description.append("\n\nHistory location diff example:");
		description.append("\n  Q: "); description.append(QuestionExampleWithLocation);
		description.append("\n  A: "); description.append(AnswerExample);

		spec.function.description = description.toString();

		var parameters = new McpToolCallParameters();
		parameters.type = "object";
		var properties = new HashMap<String, McpToolCallProperty>();

		var projectNameProp = new McpToolCallProperty();
		projectNameProp.type = "string";
		projectNameProp.description = "Project name in IDE. For example, \"MyProject\".";
		properties.put("project_name", projectNameProp);

		var filePathProp = new McpToolCallProperty();
		filePathProp.type = "string";
		filePathProp.description = "Relative file path within the project. For example, \"src/com/example/MyClass.java\". Absolute paths are also supported.";
		properties.put("file_path", filePathProp);

		var revisionIdProp = new McpToolCallProperty();
		revisionIdProp.type = "string";
		revisionIdProp.description = "Revision id from LocalHistory response. Required if history_location is not provided.";
		properties.put("revision_id", revisionIdProp);

		var historyLocationProp = new McpToolCallProperty();
		historyLocationProp.type = "string";
		historyLocationProp.description = "Absolute path to the local history file. Optional alternative to revision_id.";
		properties.put("history_location", historyLocationProp);

		var fromRevisionIdProp = new McpToolCallProperty();
		fromRevisionIdProp.type = "string";
		fromRevisionIdProp.description = "Start revision id for diff. Supports: current, latest, oldest, previous.";
		properties.put("from_revision_id", fromRevisionIdProp);

		var toRevisionIdProp = new McpToolCallProperty();
		toRevisionIdProp.type = "string";
		toRevisionIdProp.description = "Target revision id for diff. Supports: current, latest, oldest, previous.";
		properties.put("to_revision_id", toRevisionIdProp);

		var fromHistoryLocationProp = new McpToolCallProperty();
		fromHistoryLocationProp.type = "string";
		fromHistoryLocationProp.description = "Absolute path to history file for the start revision.";
		properties.put("from_history_location", fromHistoryLocationProp);

		var toHistoryLocationProp = new McpToolCallProperty();
		toHistoryLocationProp.type = "string";
		toHistoryLocationProp.description = "Absolute path to history file for the target revision.";
		properties.put("to_history_location", toHistoryLocationProp);

		var fromIndexProp = new McpToolCallProperty();
		fromIndexProp.type = "integer";
		fromIndexProp.description = "Start revision index from LocalHistory response.";
		properties.put("from_index", fromIndexProp);

		var toIndexProp = new McpToolCallProperty();
		toIndexProp.type = "integer";
		toIndexProp.description = "Target revision index from LocalHistory response.";
		properties.put("to_index", toIndexProp);

		var contextLinesProp = new McpToolCallProperty();
		contextLinesProp.type = "integer";
		contextLinesProp.description = "Number of context lines to show around changes. Default: " + DEFAULT_CONTEXT_LINES;
		properties.put("context_lines", contextLinesProp);

		var maxEntriesProp = new McpToolCallProperty();
		maxEntriesProp.type = "integer";
		maxEntriesProp.description = "Maximum number of history entries to search when using revision_id. Default: "
			+ DEFAULT_MAX_ENTRIES + ". Use 0 to search all entries.";
		properties.put("max_entries", maxEntriesProp);

		parameters.properties = properties;
		parameters.required = Arrays.asList("project_name", "file_path");
		spec.function.parameters = parameters;

		return spec;
	}

	private static class Request
	{
		@SerializedName("project_name")
		public String projectName;

		@SerializedName("file_path")
		public String filePath;

		@SerializedName("revision_id")
		public String revisionId;

		@SerializedName("history_location")
		public String historyLocation;

		@SerializedName("from_revision_id")
		public String fromRevisionId;

		@SerializedName("to_revision_id")
		public String toRevisionId;

		@SerializedName("from_history_location")
		public String fromHistoryLocation;

		@SerializedName("to_history_location")
		public String toHistoryLocation;

		@SerializedName("from_index")
		public Integer fromIndex;

		@SerializedName("to_index")
		public Integer toIndex;

		@SerializedName("context_lines")
		public Integer contextLines;

		@SerializedName("max_entries")
		public Integer maxEntries;
	}

	private static class LocalChangesResponse
	{
		@SerializedName("project_name")
		public String projectName;

		@SerializedName("file_path")
		public String filePath;

		@SerializedName("from_revision_id")
		public String fromRevisionId;

		@SerializedName("from_history_location")
		public String fromHistoryLocation;

		@SerializedName("to_revision_id")
		public String toRevisionId;

		@SerializedName("to_history_location")
		public String toHistoryLocation;

		@SerializedName("context_lines")
		public int contextLines;

		@SerializedName("has_changes")
		public boolean hasChanges;

		// Large field last so it is dropped first if the response is truncated.
		@SerializedName("diff_text")
		public String diffText;
	}

	private static class RevisionSelector
	{
		public String revisionId;
		public String historyLocation;
		public Integer index;
	}

	private static class ResolvedRevision
	{
		public final String revisionId;
		public final String historyLocation;
		private final Path path;
		private final byte[] content;

		private ResolvedRevision(Path path, byte[] content, String revisionId, String historyLocation)
		{
			this.path = path;
			this.content = content;
			this.revisionId = revisionId;
			this.historyLocation = historyLocation;
		}

		public static ResolvedRevision forPath(Path path, String revisionId, String historyLocation)
		{
			return new ResolvedRevision(path, null, revisionId, historyLocation);
		}

		public static ResolvedRevision forContent(byte[] content, String revisionId, String historyLocation)
		{
			return new ResolvedRevision(null, content, revisionId, historyLocation);
		}

        @SuppressWarnings("nls")
        public byte[] getContent()
		{
			if (content != null)
			{
				return content;
			}
			if (path == null)
			{
				throw new RuntimeException("Revision content is not available.");
			}
			try
			{
				if (!Files.exists(path))
				{
					throw new RuntimeException("History entry does not exist at \"" + path + "\".");
				}
				return Files.readAllBytes(path);
			}
			catch (Exception e)
			{
				throw new RuntimeException("Failed to read revision content: " + e.getMessage(), e);
			}
		}
	}

	private static class HistoryState
	{
		public String revisionId;
		public byte[] content;
	}
}
