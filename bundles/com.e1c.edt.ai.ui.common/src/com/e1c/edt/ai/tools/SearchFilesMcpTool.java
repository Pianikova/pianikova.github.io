/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.TextColor;
import com.e1c.edt.ai.ToolCallMessage;
import com.e1c.edt.ai.ToolCallMessageDetails;
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

public class SearchFilesMcpTool
	implements IMcpTool
{
	public static final String TOOL_NAME = "SearchFiles"; //$NON-NLS-1$
	private static final int DEFAULT_MAX_FILES = McpToolConstants.DEFAULT_MAX_FILES;

	// @formatter:off
	@SuppressWarnings("nls")
	private static String QuestionExample =
		"{\n"
		+ "  \"project_name\": \"MyProject\",\n"
		+ "  \"search_pattern\": \"*.bsl\",\n"
		+ "  \"include_subfolders\": true,\n"
		+ "  \"max_results\": 20\n"
		+ "}";

	@SuppressWarnings("nls")
	private static String AnswerExample =
		"[\n"
		+ "  {\n"
		+ "    \"project_name\": \"MyProject\",\n"
		+ "    \"relative_file_path\": \"src/CommonModules/MainModule/Module.bsl\",\n"
		+ "    \"absolute_file_path\": \"/workspace/MyProject/src/CommonModules/MainModule/Module.bsl\",\n"
		+ "    \"file_name\": \"Module.bsl\",\n"
		+ "    \"file_size\": 1024,\n"
		+ "    \"last_modified\": \"2025-01-15T10:30:00\"\n"
		+ "  }\n"
		+ "]";
	// @formatter:on

	private final IJson json;
	private final McpToolCallSpecification spec;
	private final IMcpToolsCallMessageFactory messageFactory;
	private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
	private final IMarkdownUtils markdownUtils;

	@Inject
	public SearchFilesMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
		Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IMarkdownUtils markdownUtils)
	{
		Preconditions.checkNotNull(json);
		Preconditions.checkNotNull(messageFactory);
		Preconditions.checkNotNull(cancellationProgressMonitor);
		Preconditions.checkNotNull(markdownUtils);
		this.json = json;
		this.messageFactory = messageFactory;
		this.cancellationProgressMonitor = cancellationProgressMonitor;
		this.markdownUtils = markdownUtils;
		spec = createSpecification();
	}

	@Override
	public boolean isExperimental()
	{
		return true;
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
			return CompletableFuture
				.completedFuture(messageFactory.createError(this, call,
					"Cannot deserialize arguments. Use this example: " + QuestionExample));
		}

		var request = optionalRequest.get();

		if (call.callKind == ToolCallKind.RENDER)
		{
			var pattern = request.searchPattern != null ? request.searchPattern : "*";
			details.requestMarkdown = MessageFormat.format(Messages.FindFilesTitleTemplate, pattern);
			return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
		}

		var projectName = request.projectName;
		if (projectName == null || projectName.isBlank())
		{
			return CompletableFuture
				.completedFuture(messageFactory.createError(this, call,
					"`project_name` is required."));
		}

		var searchPattern = request.searchPattern != null ? request.searchPattern : "*";
		var includeSubfolders = request.includeSubfolders != null ? request.includeSubfolders : true;
		var maxResults = request.maxResults != null && request.maxResults > 0 ? request.maxResults : DEFAULT_MAX_FILES;

		// Use supplyAsync to execute the blocking operation on a separate thread.
		return CompletableFuture.supplyAsync(() ->
		{
			// Check for cancellation before starting the work.
			if (cancellationToken.isCanceled())
			{
				return messageFactory.createError(this, call, "Operation was cancelled before execution.");
			}

			var root = ResourcesPlugin.getWorkspace().getRoot();
			var project = root.getProject(projectName);

			// Validate project existence and accessibility
			if (project == null || !project.exists())
			{
				return messageFactory.createError(this, call, "The project \"" + projectName + "\" does not exist.");
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
					return messageFactory.createError(this, call,
						"Cannot open the project \"" + projectName + "\". " + error.getMessage());
				}
			}

            var foundFiles = new ArrayList<FileInfo>();
			try
			{
				var monitor = cancellationProgressMonitor.get();
				monitor.setCancellationToken(cancellationToken);

				// Search for files matching the pattern
				searchFiles(project, searchPattern, includeSubfolders, foundFiles, maxResults, monitor, cancellationToken);
			}
			catch (CoreException e)
			{
				return messageFactory.createError(this, call, "Search failed: " + e.getMessage());
			}

			// Prepare response
			var content = json.serialize(foundFiles);

			// Create response markdown
			var responseMarkdown = new StringBuilder();
			responseMarkdown.append(MessageFormat.format(Messages.FilesFoundTemplate,
				markdownUtils.createStyledText(String.valueOf(foundFiles.size()), TextColor.GREEN, FontWeight.BOLD)));

			// Add search results in collapsible section
			responseMarkdown.append("\n\n<details><summary>").append(Messages.SearchResults).append("</summary>\n\n");

			for (var fileInfo : foundFiles)
			{
				responseMarkdown.append("- **")
					.append(markdownUtils.escapeForMarkdown(fileInfo.relativeFilePath))
					.append("**\n");

				if (fileInfo.fileSize > 0)
				{
					responseMarkdown.append("  - ")
						.append(Messages.FileSize)
						.append(": ")
						.append(formatFileSize(fileInfo.fileSize))
						.append("\n");
				}

				if (fileInfo.lastModified != null && !fileInfo.lastModified.isEmpty())
				{
					responseMarkdown.append("  - ")
						.append(Messages.LastModified)
						.append(": ")
						.append(fileInfo.lastModified)
						.append("\n");
				}

				responseMarkdown.append("\n");
			}

			responseMarkdown.append("</details>");

			details.responseMarkdown = responseMarkdown.toString();
			return messageFactory.createMessage(this, call, content, details);
		});
	}

	private void searchFiles(IResource container, String pattern, boolean includeSubfolders,
		List<FileInfo> foundFiles, int maxResults, ICancellationProgressMonitor monitor, ICancellationToken cancellationToken)
		throws CoreException
	{
		if (cancellationToken.isCanceled() || foundFiles.size() >= maxResults)
		{
			return;
		}

		try
		{
			// Check for cancellation before each directory
			if (cancellationToken.isCanceled())
			{
				return;
			}

			IResource[] members;
			if (container.getType() == IResource.PROJECT || container.getType() == IResource.FOLDER)
			{
				members = ((org.eclipse.core.resources.IContainer) container).members();
			}
			else
			{
				return;
			}
			for (IResource member : members)
			{
				if (cancellationToken.isCanceled() || foundFiles.size() >= maxResults)
				{
					return;
				}

				if (member.getType() == IResource.FILE)
				{
					// Check if file matches the pattern
					if (matchesPattern(member.getName(), pattern))
					{
						var fileInfo = new FileInfo();
						fileInfo.projectName = member.getProject().getName();
						fileInfo.relativeFilePath = member.getProjectRelativePath().toPortableString();
						fileInfo.absoluteFilePath = member.getLocation().toOSString();
						fileInfo.fileName = member.getName();
						fileInfo.fileSize = member.getLocation().toFile().length();
						fileInfo.lastModified = new java.text.SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss") //$NON-NLS-1$
							.format(new java.util.Date(member.getLocalTimeStamp()));

						foundFiles.add(fileInfo);
					}
				}
				else if (includeSubfolders && member.getType() == IResource.FOLDER)
				{
					// Recursively search subfolders
					searchFiles(member, pattern, includeSubfolders, foundFiles, maxResults, monitor, cancellationToken);
				}
			}
		}
		catch (CoreException e)
		{
			// Log the error but continue searching other files
			// In a real implementation, you might want to handle this differently
		}
	}

	private boolean matchesPattern(String fileName, String pattern)
	{
		// Simple wildcard pattern matching
		// Supports * (matches any number of characters) and ? (matches exactly one character)

		// Convert pattern to regex
        var regex = pattern.replace(".", "\\.") //$NON-NLS-1$ //$NON-NLS-2$
							 .replace("*", ".*") //$NON-NLS-1$ //$NON-NLS-2$
							 .replace("?", "."); //$NON-NLS-1$ //$NON-NLS-2$
		return fileName.matches(regex);
	}

	private String formatFileSize(long bytes)
	{
		if (bytes < 1024)
		{
			return bytes + " B"; //$NON-NLS-1$
		}
		else if (bytes < 1024 * 1024)
		{
			return String.format("%.1f KB", bytes / 1024.0); //$NON-NLS-1$
		}
		else
		{
			return String.format("%.1f MB", bytes / (1024.0 * 1024.0)); //$NON-NLS-1$
		}
	}

	@SuppressWarnings("nls")
	private static McpToolCallSpecification createSpecification()
	{
		// @formatter:off
		var spec = new McpToolCallSpecification();
		spec.type = "function";
		spec.function = new McpToolCallFunction();
		spec.function.name = TOOL_NAME;

		var description = new StringBuilder();
		description.append("Finds files by name pattern in a project.");
		description.append("\n\nUsage:");
		description.append("\n- Supports wildcards: `*` for any characters, `?` for a single character.");
		description.append("\n- Can search recursively or only in the root folder.");
		description.append("\n- Returns file size and last modified date.");
		description.append("\n- Limits results to avoid overload on large projects.");
		description.append("\n\nRelated tools:");
		description.append("\n- Search by content: `" + FindMcpTool.TOOL_NAME + "`.");
		description.append("\n- Open/edit files: `" + ReadMcpTool.TOOL_NAME + "`, `" + EditMcpTool.TOOL_NAME + "`.");
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

		var searchPatternProp = new McpToolCallProperty();
		searchPatternProp.type = "string";
		searchPatternProp.description = "File name search pattern. Supports wildcards (*, ?). Default: \"*\" (all files).";
		properties.put("search_pattern", searchPatternProp);

		var includeSubfoldersProp = new McpToolCallProperty();
		includeSubfoldersProp.type = "boolean";
		includeSubfoldersProp.description = "Include subfolders in search. Default: true";
		properties.put("include_subfolders", includeSubfoldersProp);

		var maxResultsProp = new McpToolCallProperty();
		maxResultsProp.type = "integer";
		maxResultsProp.description = "Maximum number of results to return. Default: " + DEFAULT_MAX_FILES;
		properties.put("max_results", maxResultsProp);

		parameters.properties = properties;
		parameters.required = Arrays.asList("project_name");
		spec.function.parameters = parameters;

		return spec;
		// @formatter:on
	}

	private static class Request
	{
		/**
		 * Project name in IDE.
		 */
		@SerializedName("project_name")
		public String projectName;

		/**
		 * File name search pattern with wildcards.
		 */
		@SerializedName("search_pattern")
		public String searchPattern;

		/**
		 * Include subfolders in search.
		 */
		@SerializedName("include_subfolders")
		public Boolean includeSubfolders;

		/**
		 * Maximum number of results to return.
		 */
		@SerializedName("max_results")
		public Integer maxResults;
	}

	private static class FileInfo
	{
		/**
		 * Name of the project.
		 */
		@SerializedName("project_name")
		public String projectName;

		/**
		 * Project relative path to the file.
		 */
		@SerializedName("relative_file_path")
		public String relativeFilePath;

		/**
		 * Absolute file system path.
		 */
		@SerializedName("absolute_file_path")
		public String absoluteFilePath;

		/**
		 * File name only.
		 */
		@SerializedName("file_name")
		public String fileName;

		/**
		 * File size in bytes.
		 */
		@SerializedName("file_size")
		public long fileSize;

		/**
		 * Last modified timestamp (ISO format).
		 */
		@SerializedName("last_modified")
		public String lastModified;
	}
}

