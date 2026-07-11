/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

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

public class LocalHistoryMcpTool
	implements IMcpTool
{
	public static final String TOOL_NAME = "LocalHistory"; //$NON-NLS-1$
	private static final int DEFAULT_MAX_ENTRIES = McpToolConstants.DEFAULT_MAX_HISTORY_ENTRIES;

	// @formatter:off
	@SuppressWarnings("nls")
	private static String QuestionExample =
		"{\n"
		+ "  \"project_name\": \"MyProject\",\n"
		+ "  \"file_path\": \"src/com/example/MyClass.java\",\n"
		+ "  \"max_entries\": 10\n"
		+ "}";

	@SuppressWarnings("nls")
	private static String AnswerExample =
		"{\n"
		+ "  \"has_more\": true,\n"
		+ "  \"current_is_dirty\": true,\n"
		+ "  \"differs_from_latest\": true,\n"
		+ "  \"entries\": [\n"
		+ "    {\n"
		+ "      \"index\": 0,\n"
		+ "      \"revision_id\": \"current\",\n"
		+ "      \"timestamp\": 1642678800000,\n"
		+ "      \"formatted_time\": \"2022-01-20T10:30:45+03:00\",\n"
		+ "      \"file_size\": 1024,\n"
		+ "      \"location\": \"/path/to/file\",\n"
		+ "      \"is_current\": true,\n"
		+ "      \"is_dirty\": true,\n"
		+ "      \"is_oldest\": false\n"
		+ "    }\n"
		+ "  ]\n"
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
	public LocalHistoryMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
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

        if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = MessageFormat.format(Messages.LocalHistoryTitleTemplate,
                projectName != null ? projectName : Messages.CurrentProject, filePath != null ? filePath : Messages.SelectedFile);
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        return CompletableFuture.supplyAsync(() ->
		{
			try
			{
				// Check cancellation first
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

				// Check cancellation before expensive operation
				if (cancellationToken.isCanceled())
				{
					throw new ToolException("Operation was cancelled before retrieving history.");
				}

				// Retrieve local history limited to requested number
				List<LocalHistoryEntry> historyEntries;
				try
				{
					historyEntries = localHistoryUtils.getLocalHistory(actualFile, maxEntries);
				}
				catch (Exception e)
				{
					throw new ToolException("Failed to get local history: " + e.getMessage(), e, ToolErrorType.RETRYABLE);
				}

				// Check cancellation after retrieving history
				if (cancellationToken.isCanceled())
				{
					throw new ToolException("Operation was cancelled while processing history.");
				}

			// Index history entries
			var lastIndex = historyEntries.size() - 1;
			for (int i = 0; i < historyEntries.size(); i++)
			{
				var entry = historyEntries.get(i);
				entry.index = i;
				entry.isOldest = i == lastIndex;
			}

                // Determine if there are more history entries
                boolean hasMore = historyEntries.size() == maxEntries && maxEntries != Integer.MAX_VALUE;

			var response = new LocalHistoryResponse();
			response.entries = historyEntries;
                response.hasMore = hasMore;
                response.currentIsDirty = !historyEntries.isEmpty() && historyEntries.get(0).isDirty;
                response.differsFromLatest = localHistoryUtils.currentDiffersFromLatest(actualFile).orElse(null);

			var content = json.serialize(response);

			// Build response markdown
			var responseMarkdown = new StringBuilder();
			responseMarkdown.append(MessageFormat.format(Messages.LocalHistoryFoundTemplate,
                    markdownUtils.createStyledText(String.valueOf(historyEntries.size()), TextColor.GREEN,
                        FontWeight.BOLD, false),
				markdownUtils.escapeForMarkdown(filePath),
				markdownUtils.escapeForMarkdown(projectName)));

			if (!historyEntries.isEmpty())
			{
				responseMarkdown.append("\n\n**").append(Messages.ViewHistory).append("**\n\n");

				for (var entry : historyEntries)
				{
					responseMarkdown.append("### **")
                            .append(markdownUtils.createStyledText(entry.revisionId, TextColor.BLUE, FontWeight.NORMAL,
                                false))
						.append("**")
						.append(entry.isCurrent ? " " + Messages.Current : "")
						.append(entry.isDirty ? " " + Messages.UnsavedChanges : "")
						.append(" - ")
						.append(entry.formattedTime)
						.append("\n\n");

					responseMarkdown.append("**")
						.append(Messages.FileSize)
						.append(":** ")
						.append(entry.fileSize)
						.append(" bytes\n");

					responseMarkdown.append("**")
						.append(Messages.Location)
						.append(":** ")
						.append(markdownUtils.escapeForMarkdown(entry.location))
						.append("\n\n");

					responseMarkdown.append("---\n\n");
				}
			}
			else
			{
				responseMarkdown.append("\n\n").append(Messages.NoLocalHistoryFound);
			}

			details.responseMarkdown = responseMarkdown.toString();
			return messageFactory.createMessage(this, call, content, details);
			}
			catch (Exception e)
			{
				throw new ToolException("Failed to get local history: " + e.getMessage(), e, ToolErrorType.RETRYABLE);
			}
		});
	}

	@SuppressWarnings("nls")
	private static McpToolCallSpecification createSpecification()
	{
		var spec = new McpToolCallSpecification();
		spec.type = "function";
		spec.function = new McpToolCallFunction();
		spec.function.name = TOOL_NAME;

		var description = new StringBuilder();
		description.append("Lists local history revisions for a file. "
			+ "Think of it as `git log` for the IDE local history: it works even without a Git repository.");
		description.append("\n\nUsage:");
		description.append("\n- Arguments must be a single JSON object.");
		description.append("\n- Returns recent entries first (index 0 is current).");
		description.append("\n- `current` (index 0) is the live state of the file, including unsaved editor changes"
			+ " (`is_dirty` is true when the editor has unsaved changes); it is like the working tree in Git,"
			+ " while `latest` history entry is like HEAD.");
		description.append("\n- Includes timestamp, size, and history location.");
		description.append("\n- Each entry includes `index` and `is_oldest` to help select versions.");
		description.append("\n- `location` is a virtual id (`local_history:<revision_id>`) for history entries.");
		description.append("\n- Set `max_entries` to 0 to return all available history.");
		description.append("\n- Works with Eclipse local history when available.");
		description.append("\n- Response includes has_more flag indicating if more history entries are available.");
		description.append("\n- Response includes `current_is_dirty` (unsaved editor changes exist) and"
			+ " `differs_from_latest` (current content differs from the newest history revision; null when there is no history).");
		description.append("\n\nRelated tools:");
		description.append("\n- Diff revisions: `" + LocalChangesMcpTool.TOOL_NAME
			+ "` (like `git diff`). To see changes not yet recorded in history, call it without revision selectors"
			+ " or with from_revision_id=\"latest\", to_revision_id=\"current\".");
		description.append("\n\nExample:");
		description.append("\n  Q: "); description.append(QuestionExample);
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

		var maxEntriesProp = new McpToolCallProperty();
		maxEntriesProp.type = "integer";
		maxEntriesProp.description = "Maximum number of history entries to return. Default: " + DEFAULT_MAX_ENTRIES
			+ ". Use 0 to return all entries.";
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

		@SerializedName("max_entries")
		public Integer maxEntries;
	}

	private static class LocalHistoryResponse
	{
		@SerializedName("has_more")
		public boolean hasMore;

		@SerializedName("current_is_dirty")
		public boolean currentIsDirty;

		@SerializedName("differs_from_latest")
		public Boolean differsFromLatest;

		// Large field last so it is dropped first if the response is truncated.
		@SerializedName("entries")
		public List<LocalHistoryEntry> entries;
	}
}
