/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
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

public class GitDiffMcpTool
	implements IMcpTool
{
	public static final String TOOL_NAME = "GitDiff"; //$NON-NLS-1$
	private static final int DEFAULT_CONTEXT_LINES = McpToolConstants.DEFAULT_GIT_DIFF_CONTEXT_LINES;

	// @formatter:off
	@SuppressWarnings("nls")
	private static String QuestionExample =
		"{\n"
		+ "  \"project_name\": \"MyProject\",\n"
		+ "  \"context_lines\": 5\n"
		+ "}";

	@SuppressWarnings("nls")
	private static String QuestionExampleUncommitted =
		"{\n"
		+ "  \"project_name\": \"MyProject\",\n"
		+ "  \"uncommitted_changes\": true,\n"
		+ "  \"context_lines\": 5\n"
		+ "}";

	@SuppressWarnings("nls")
	private static String QuestionExampleWithCommits =
		"{\n"
		+ "  \"project_name\": \"MyProject\",\n"
		+ "  \"old_commit\": \"a1b2c3d4\",\n"
		+ "  \"new_commit\": \"e5f6g7h8\",\n"
		+ "  \"context_lines\": 5\n"
		+ "}";

	@SuppressWarnings("nls")
	private static String AnswerExample =
		"{\n"
		+ "  \"project_name\": \"MyProject\",\n"
		+ "  \"diff_text\": \"diff --git a/src/example.java b/src/example.java\\nindex 1234567..abcdefg 100644\\n--- a/src/example.java\\n+++ b/src/example.java\\n@@ -1,5 +1,5 @@\\n public class Example {\\n-    private int oldField;\\n+    private int newField;\\n }\",\n"
		+ "  \"context_lines\": 5,\n"
		+ "  \"has_changes\": true\n"
		+ "}";

	@SuppressWarnings("nls")
	private static String AnswerExampleUncommitted =
		"{\n"
		+ "  \"project_name\": \"MyProject\",\n"
		+ "  \"uncommitted_changes\": true,\n"
		+ "  \"diff_text\": \"diff --git a/src/example.java b/src/example.java\\nindex 1234567..abcdefg 100644\\n--- a/src/example.java\\n+++ b/src/example.java\\n@@ -1,5 +1,5 @@\\n public class Example {\\n-    private int oldField;\\n+    private int newField;\\n }\",\n"
		+ "  \"context_lines\": 5,\n"
		+ "  \"has_changes\": true\n"
		+ "}";

	@SuppressWarnings("nls")
	private static String AnswerExampleWithCommits =
		"{\n"
		+ "  \"project_name\": \"MyProject\",\n"
		+ "  \"old_commit\": \"a1b2c3d4\",\n"
		+ "  \"new_commit\": \"e5f6g7h8\",\n"
		+ "  \"diff_text\": \"diff --git a/src/example.java b/src/example.java\\nindex 1234567..abcdefg 100644\\n--- a/src/example.java\\n+++ b/src/example.java\\n@@ -1,5 +1,5 @@\\n public class Example {\\n-    private int oldField;\\n+    private int newField;\\n }\",\n"
		+ "  \"context_lines\": 5,\n"
		+ "  \"has_changes\": true\n"
		+ "}";
	// @formatter:on

	private final IJson json;
	private final McpToolCallSpecification spec;
	private final IMcpToolsCallMessageFactory messageFactory;
	private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
	private final IMarkdownUtils markdownUtils;
	private final IGitTools gitTools;

	@Inject
	public GitDiffMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
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

		var contextLines = request.contextLines != null && request.contextLines > 0 ? request.contextLines : DEFAULT_CONTEXT_LINES;
		var oldCommit = request.oldCommit;
		var newCommit = request.newCommit;
		var uncommittedChanges = Boolean.TRUE.equals(request.uncommittedChanges);

        if (call.callKind == ToolCallKind.RENDER)
        {
            details.requestMarkdown = MessageFormat.format(Messages.GitDiffTitleTemplate,
                projectName != null ? projectName : "current project");
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
					throw new ToolException("Cannot open the project \"" + projectName + "\"", error, ToolErrorType.RETRYABLE);
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
				// Get diff text
				String diffText;
				if (oldCommit != null && newCommit != null)
				{
					// Get diff between specific commits
					diffText = gitTools.getDiffText(repository, oldCommit, newCommit, contextLines);
				}
				else if (uncommittedChanges)
				{
					// Get diff for uncommitted changes (staged + unstaged)
					diffText = gitTools.getUncommittedDiffText(repository, contextLines);
				}
				else
				{
					// Get staged diff (index vs HEAD)
					diffText = gitTools.getDiffText(repository, contextLines);
				}

				var hasChanges = !diffText.trim().isEmpty();

				// Prepare response
				var response = new GitDiffResponse();
				response.projectName = projectName;
				response.oldCommit = oldCommit;
				response.newCommit = newCommit;
				response.uncommittedChanges = oldCommit == null && newCommit == null ? uncommittedChanges : null;
				response.diffText = diffText;
				response.contextLines = contextLines;
				response.hasChanges = hasChanges;

				var content = json.serialize(response);

				// Create response markdown
				var responseMarkdown = new StringBuilder();

				if (hasChanges)
				{
					String diffType;
					if (oldCommit != null && newCommit != null)
					{
						diffType = MessageFormat.format(Messages.GitCommitDiffTemplate,
                            markdownUtils.createStyledText(
                                oldCommit.substring(0, Math.min(8, oldCommit.length())), TextColor.BLUE,
                                FontWeight.NORMAL, true),
                            markdownUtils.createStyledText(
                                newCommit.substring(0, Math.min(8, newCommit.length())), TextColor.BLUE,
                                FontWeight.NORMAL, true),
							markdownUtils.escapeForMarkdown(projectName));
					}
					else if (uncommittedChanges)
					{
						diffType = MessageFormat.format(Messages.GitUncommittedDiffTemplate,
                            markdownUtils.createStyledText(String.valueOf(contextLines), TextColor.GREEN,
                                FontWeight.BOLD, false),
							markdownUtils.escapeForMarkdown(projectName));
					}
					else
					{
						diffType = MessageFormat.format(Messages.GitDiffFoundTemplate,
                            markdownUtils.createStyledText(String.valueOf(contextLines), TextColor.GREEN,
                                FontWeight.BOLD, false),
							markdownUtils.escapeForMarkdown(projectName));
					}

					responseMarkdown.append(diffType);
					responseMarkdown.append("\n\n");
					responseMarkdown.append(markdownUtils.buildUnifiedDiffByFile(diffText));
				}
				else
				{
					if (oldCommit != null && newCommit != null)
					{
						responseMarkdown.append(MessageFormat.format(Messages.NoGitCommitChangesTemplate,
                            markdownUtils.createStyledText(oldCommit.substring(0, Math.min(8, oldCommit.length())),
                                TextColor.BLUE, FontWeight.BOLD, true),
                            markdownUtils.createStyledText(newCommit.substring(0, Math.min(8, newCommit.length())),
                                TextColor.BLUE, FontWeight.BOLD, true)));
					}
					else if (uncommittedChanges)
					{
						responseMarkdown.append(MessageFormat.format(Messages.NoGitUncommittedChangesTemplate,
							markdownUtils.escapeForMarkdown(projectName)));
					}
					else
					{
						responseMarkdown.append(MessageFormat.format(Messages.NoGitChangesTemplate,
							markdownUtils.escapeForMarkdown(projectName)));
					}
				}

				details.responseMarkdown = responseMarkdown.toString();
				return messageFactory.createMessage(this, call, content, details);
			}
			catch (Exception e)
			{
				throw new ToolException("Failed to get Git diff", e, ToolErrorType.RETRYABLE);
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
		description.append("Retrieves Git diffs for working directory changes or commit ranges.");
		description.append("\n\nUsage:");
		description.append("\n- Arguments must be a single JSON object.");
		description.append("\n- Default: staged vs last commit.");
		description.append("\n- Set `uncommitted_changes` to include staged + unstaged.");
		description.append("\n- Provide `old_commit` and `new_commit` to diff specific commits.");
		description.append("\n- `context_lines` controls surrounding lines in the diff.");
		description.append("\n- Requires the project to be a Git repository.");
		description.append("\n\nRelated tools:");
		description.append("\n- Get commit hashes: `" + GitCommitsMcpTool.TOOL_NAME + "`.");
		description.append("\n- Non-Git history: `" + LocalHistoryMcpTool.TOOL_NAME + "`, `" + LocalChangesMcpTool.TOOL_NAME + "`.");
		description.append("\n\nStaged changes diff example:");
		description.append("\n  Q: "); description.append(QuestionExample);
		description.append("\n  A: "); description.append(AnswerExample);
		description.append("\n\nUncommitted changes diff example:");
		description.append("\n  Q: "); description.append(QuestionExampleUncommitted);
		description.append("\n  A: "); description.append(AnswerExampleUncommitted);
		description.append("\n\nCommit comparison example:");
		description.append("\n  Q: "); description.append(QuestionExampleWithCommits);
		description.append("\n  A: "); description.append(AnswerExampleWithCommits);

		spec.function.description = description.toString();

		var parameters = new McpToolCallParameters();
		parameters.type = "object";
		var properties = new HashMap<String, McpToolCallProperty>();

		var projectNameProp = new McpToolCallProperty();
		projectNameProp.type = "string";
		projectNameProp.description = "Project name in IDE. For example, \"MyProject\".";
		properties.put("project_name", projectNameProp);

		var contextLinesProp = new McpToolCallProperty();
		contextLinesProp.type = "integer";
		contextLinesProp.description = "Number of context lines to show around changes. Default: " + DEFAULT_CONTEXT_LINES;
		properties.put("context_lines", contextLinesProp);

		var oldCommitProp = new McpToolCallProperty();
		oldCommitProp.type = "string";
		oldCommitProp.description = "Old commit hash (optional). If provided with new_commit, shows diff between these two commits instead of working directory changes.";
		properties.put("old_commit", oldCommitProp);

		var newCommitProp = new McpToolCallProperty();
		newCommitProp.type = "string";
		newCommitProp.description = "New commit hash (optional). If provided with old_commit, shows diff between these two commits instead of working directory changes.";
		properties.put("new_commit", newCommitProp);

		var uncommittedChangesProp = new McpToolCallProperty();
		uncommittedChangesProp.type = "boolean";
		uncommittedChangesProp.description = "If true, returns diff for uncommitted changes (staged + unstaged) instead of only staged changes.";
		properties.put("uncommitted_changes", uncommittedChangesProp);

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
		 * Number of context lines to show around changes.
		 */
		@SerializedName("context_lines")
		public Integer contextLines;

		/**
		 * Old commit hash (optional).
		 */
		@SerializedName("old_commit")
		public String oldCommit;

		/**
		 * New commit hash (optional).
		 */
		@SerializedName("new_commit")
		public String newCommit;

		/**
		 * When true, return uncommitted changes (staged + unstaged).
		 */
		@SerializedName("uncommitted_changes")
		public Boolean uncommittedChanges;
	}

	private static class GitDiffResponse
	{
		/**
		 * Name of the project.
		 */
		@SerializedName("project_name")
		public String projectName;

		/**
		 * Old commit hash (if comparing commits).
		 */
		@SerializedName("old_commit")
		public String oldCommit;

		/**
		 * New commit hash (if comparing commits).
		 */
		@SerializedName("new_commit")
		public String newCommit;

		/**
		 * Whether uncommitted changes were requested.
		 */
		@SerializedName("uncommitted_changes")
		public Boolean uncommittedChanges;

		/**
		 * Git diff content in standard format.
		 */
		@SerializedName("diff_text")
		public String diffText;

		/**
		 * Number of context lines used.
		 */
		@SerializedName("context_lines")
		public int contextLines;

		/**
		 * Whether there are any changes.
		 */
		@SerializedName("has_changes")
		public boolean hasChanges;
	}
}
