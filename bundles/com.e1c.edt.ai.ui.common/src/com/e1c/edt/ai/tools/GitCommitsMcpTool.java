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
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.model.McpToolCall;
import com.e1c.edt.ai.assistent.model.McpToolCallFunction;
import com.e1c.edt.ai.assistent.model.McpToolCallParameters;
import com.e1c.edt.ai.assistent.model.McpToolCallProperty;
import com.e1c.edt.ai.assistent.model.McpToolCallSpecification;
import com.e1c.edt.ai.assistent.model.ToolCallKind;
import com.e1c.edt.ai.ui.GitUtils;
import com.e1c.edt.ai.ui.IGitTools;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class GitCommitsMcpTool
	implements IMcpTool
{
	public static final String TOOL_NAME = "GitCommits"; //$NON-NLS-1$
    private static final int DEFAULT_MAX_COMMITS = McpToolConstants.DEFAULT_MAX_GIT_COMMITS;

	// @formatter:off
	@SuppressWarnings("nls")
	private static String QuestionExample =
		"{\n"
		+ "  \"project_name\": \"MyProject\",\n"
		+ "  \"max_commits\": 10\n"
		+ "}";

	@SuppressWarnings("nls")
	private static String AnswerExample =
		"{\n"
		+ "  \"commits\": [\n"
		+ "    {\n"
		+ "      \"hash\": \"a1b2c3d4e5f6g7h8i9j0\",\n"
		+ "      \"short_hash\": \"a1b2c3d4\",\n"
		+ "      \"author_name\": \"John Doe\",\n"
		+ "      \"author_email\": \"john@example.com\",\n"
		+ "      \"commit_time\": 1642678800000,\n"
		+ "      \"formatted_time\": \"2022-01-20T10:30:00+03:00\",\n"
		+ "      \"message\": \"Fix bug in user authentication\",\n"
		+ "      \"changed_files\": [\"src/auth/UserService.java\", \"src/auth/UserController.java\"]\n"
		+ "    }\n"
		+ "  ],\n"
		+ "  \"has_more\": true\n"
		+ "}";
	// @formatter:on

	private final IJson json;
	private final McpToolCallSpecification spec;
	private final IMcpToolsCallMessageFactory messageFactory;
	private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
	private final IMarkdownUtils markdownUtils;
	private final IGitTools gitTools;

	@Inject
	public GitCommitsMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
		Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IMarkdownUtils markdownUtils,
		IGitTools gitTools)
	{
		Preconditions.checkNotNull(json);
		Preconditions.checkNotNull(messageFactory);
		Preconditions.checkNotNull(cancellationProgressMonitor);
		Preconditions.checkNotNull(markdownUtils);
		Preconditions.checkNotNull(gitTools);
		this.json = json;
		this.messageFactory = messageFactory;
		this.cancellationProgressMonitor = cancellationProgressMonitor;
		this.markdownUtils = markdownUtils;
		this.gitTools = gitTools;
		spec = createSpecification();
	}

	@Override
	public boolean isExperimental()
	{
		return false;
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
			throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample
				+ "\n\nRequired field: 'project_name' (string)"
				+ "\nOptional fields: 'max_commits' (integer)", ToolErrorType.RETRYABLE);
		}

		var request = optionalRequest.get();
        var projectName = request.projectName;
        if (projectName == null || projectName.isBlank())
        {
            throw new ToolException("`project_name` is required.");
        }

        var maxCommits =
            request.maxCommits != null && request.maxCommits > 0 ? request.maxCommits : DEFAULT_MAX_COMMITS;

		if (call.callKind == ToolCallKind.RENDER)
		{
			// Create detailed request markdown with search parameters
			var requestMarkdown = new StringBuilder();
			requestMarkdown.append(MessageFormat.format(Messages.GitCommitsTitleTemplate, projectName))
				.append("\n\n") //$NON-NLS-1$
				.append(Messages.ProjectName)
				.append(": ") //$NON-NLS-1$
				.append("`") //$NON-NLS-1$
				.append(markdownUtils.escapeForMarkdown(projectName))
				.append("`"); //$NON-NLS-1$

			// Add max commits parameter
			requestMarkdown.append("\n\n") //$NON-NLS-1$
				.append(Messages.MaxCommits)
				.append(": ") //$NON-NLS-1$
				.append("`") //$NON-NLS-1$
				.append(maxCommits)
				.append("`"); //$NON-NLS-1$

			details.requestMarkdown = requestMarkdown.toString();
			return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

		// Use supplyAsync to execute the blocking operation on a separate thread.
		return CompletableFuture.supplyAsync(() ->
		{
			// Check for cancellation before starting the work.
			if (cancellationToken.isCanceled())
			{
				throw new ToolException("Operation was cancelled before execution.");
			}

			var root = ResourcesPlugin.getWorkspace().getRoot();
			var project = root.getProject(projectName);

			// Validate project existence and accessibility
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
					throw new ToolException("Cannot open the project \"" + projectName + "\"", error,
						ToolErrorType.RETRYABLE);
				}
			}

			// Get Git repository for the project
			var repository = GitUtils.getRepository(project);
			if (repository == null)
			{
				throw new ToolException("The project \"" + projectName + "\" is not a Git repository.");
			}

			try
			{
				// Get commit history limited to requested number
				var gitCommits = gitTools.getCommitHistory(repository, maxCommits);
				var commitInfos = new ArrayList<CommitInfo>();

				for (var gitCommit : gitCommits)
				{
					var commitInfo = new CommitInfo();
					commitInfo.hash = gitCommit.getHash();
					commitInfo.shortHash = gitCommit.getShortHash();
					commitInfo.authorName = gitCommit.getAuthorName();
					commitInfo.authorEmail = gitCommit.getAuthorEmail();
					commitInfo.commitTime = gitCommit.getCommitTime();
					commitInfo.formattedTime = gitCommit.getFormattedTime();
					commitInfo.message = gitCommit.getMessage();
					commitInfo.changedFiles = gitCommit.getChangedFiles();
					commitInfo.changedFilesCount = gitCommit.getChangedFilesCount();

					commitInfos.add(commitInfo);
				}

				// Determine if there are more commits
				boolean hasMore = commitInfos.size() == maxCommits;

				// Create response object with commits and has_more flag
				var response = new GitCommitsResponse();
				response.commits = commitInfos;
				response.hasMore = hasMore;

				// Prepare response
				var content = json.serialize(response);

				// Create response markdown
				var responseMarkdown = new StringBuilder();
				responseMarkdown.append(MessageFormat.format(Messages.GitCommitsFoundTemplate,
					markdownUtils.createStyledText(String.valueOf(commitInfos.size()), TextColor.GREEN, FontWeight.BOLD),
					markdownUtils.escapeForMarkdown(projectName)))
					.append("\n\n") //$NON-NLS-1$
					.append(Messages.ProjectName)
					.append(": ") //$NON-NLS-1$
					.append("`") //$NON-NLS-1$
					.append(markdownUtils.escapeForMarkdown(projectName))
					.append("`") //$NON-NLS-1$
					.append("\n\n") //$NON-NLS-1$
					.append(Messages.MaxCommits + ": ") //$NON-NLS-1$
					.append("`") //$NON-NLS-1$
					.append(maxCommits)
					.append("`"); //$NON-NLS-1$

				// Add search results in collapsible section
				responseMarkdown.append("\n\n<details><summary>").append(Messages.CommitsList).append("</summary>\n\n"); //$NON-NLS-1$ //$NON-NLS-2$

				for (var commit : commitInfos)
				{
					responseMarkdown.append("### **") //$NON-NLS-1$
                        .append(markdownUtils.createStyledText(commit.shortHash, TextColor.BLUE, FontWeight.NORMAL))
						.append("** - ") //$NON-NLS-1$
						.append(markdownUtils.escapeForMarkdown(commit.message))
						.append("\n\n"); //$NON-NLS-1$

					responseMarkdown.append("**") //$NON-NLS-1$
						.append(Messages.Author)
						.append(":** ") //$NON-NLS-1$
						.append(markdownUtils.escapeForMarkdown(commit.authorName))
						.append(" <") //$NON-NLS-1$
						.append(markdownUtils.escapeForMarkdown(commit.authorEmail))
						.append(">\n"); //$NON-NLS-1$

					responseMarkdown.append("**") //$NON-NLS-1$
						.append(Messages.Date)
						.append(":** ") //$NON-NLS-1$
						.append(commit.formattedTime)
						.append("\n"); //$NON-NLS-1$

					responseMarkdown.append("**") //$NON-NLS-1$
						.append(Messages.ChangedFiles)
						.append(":** ") //$NON-NLS-1$
						.append(commit.changedFilesCount)
						.append(" ") //$NON-NLS-1$
						.append(Messages.Files.toLowerCase())
						.append("\n"); //$NON-NLS-1$

					if (!commit.changedFiles.isEmpty())
					{
						responseMarkdown.append("\n```\n");
						for (var file : commit.changedFiles)
						{
							responseMarkdown.append(markdownUtils.escapeForMarkdown(file)).append("\n");
						}
						responseMarkdown.append("```\n");
					}

					responseMarkdown.append("\n---\n\n");
				}

				responseMarkdown.append("</details>");

				details.responseMarkdown = responseMarkdown.toString();
				return messageFactory.createMessage(this, call, content, details);
			}
			catch (Exception e)
			{
				throw new ToolException("Failed to get commit history", e, ToolErrorType.RETRYABLE);
			}
		}).exceptionally(throwable ->
		{
			if (throwable.getCause() instanceof ToolException)
			{
				throw (ToolException) throwable.getCause();
			}
			throw new ToolException(throwable.getMessage(), throwable, ToolErrorType.RETRYABLE);
		});
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
		description.append("Lists recent Git commits for a project.");
		description.append("\n\nUsage:");
		description.append("\n- Arguments must be a single JSON object.");
		description.append("\n- Requires the project to be a Git repository.");
		description.append("\n- Supports limiting the number of commits.");
		description.append("\n- Returns hash, author, date, message, and changed files.");
		description.append("\n- Response includes has_more flag indicating if more commits are available.");
		description.append("\n\nRelated tools:");
		description.append("\n- Inspect diffs: `" + GitDiffMcpTool.TOOL_NAME + "`.");
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

		var maxCommitsProp = new McpToolCallProperty();
		maxCommitsProp.type = "integer";
		maxCommitsProp.description = "Maximum number of commits to return. Default: " + DEFAULT_MAX_COMMITS;
		properties.put("max_commits", maxCommitsProp);

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
		 * Maximum number of commits to return.
		 */
		@SerializedName("max_commits")
		public Integer maxCommits;
	}

	private static class CommitInfo
	{
		/**
		 * Full commit hash.
		 */
		@SerializedName("hash")
		public String hash;

		/**
		 * Short commit hash.
		 */
		@SerializedName("short_hash")
		public String shortHash;

		/**
		 * Author name.
		 */
		@SerializedName("author_name")
		public String authorName;

		/**
		 * Author email.
		 */
		@SerializedName("author_email")
		public String authorEmail;

		/**
		 * Commit timestamp in epoch milliseconds.
		 */
		@SerializedName("commit_time")
		public long commitTime;

		/**
		 * Formatted commit time (ISO format).
		 */
		@SerializedName("formatted_time")
		public String formattedTime;

		/**
		 * Commit message.
		 */
		@SerializedName("message")
		public String message;

		/**
		 * List of changed files in this commit.
		 */
		@SerializedName("changed_files")
		public List<String> changedFiles;

		/**
		 * Number of changed files.
		 */
		@SerializedName("changed_files_count")
		public int changedFilesCount;
	}

	private static class GitCommitsResponse
	{
		/**
		 * List of commits returned.
		 */
		@SerializedName("commits")
		public List<CommitInfo> commits;

		/**
		 * Indicates whether there are more commits available to retrieve.
		 */
		@SerializedName("has_more")
		public boolean hasMore;
	}
}

