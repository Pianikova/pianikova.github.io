/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import com.e1c.edt.ai.FontWeight;
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
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;

public class ListMcpTool
	implements IMcpTool
{
	public static final String TOOL_NAME = "List"; //$NON-NLS-1$

	private static final int LIMIT = 100;

    @SuppressWarnings("nls")
    private static final List<String> IGNORE_PATTERNS = Arrays.asList(
		"node_modules/",
		"__pycache__/",
		".git/",
		"dist/",
		"build/",
		"target/",
		"vendor/",
		"bin/",
		"obj/",
		".idea/",
		".vscode/",
		".zig-cache/",
		"zig-out",
		".coverage",
		"coverage/",
		"tmp/",
		"temp/",
		".cache/",
		"cache/",
		"logs/",
		".venv/",
		"venv/",
		"env/",
		".metadata/",
		".recommenders/",
		".settings/"
	);

	@SuppressWarnings("nls")
	private static String QuestionExample =
		"{\n"
			+ "  \"path\": \"/home/user/workspace\"\n"
			+ "}";

	@SuppressWarnings("nls")
	private static String AnswerExample =
		"/home/user/workspace/\n"
			+ "Documents/\n"
			+ " ├── Document1/\n"
			+ " │   ├── ManagerModule.bsl\n"
			+ " │   ├── ObjectModule.bsl\n"
			+ " │   └── Document1.mdo\n"
			+ " └── ...";

	private final IJson json;
	private final McpToolCallSpecification spec;
	private final IMcpToolsCallMessageFactory messageFactory;
    private final IMarkdownUtils markdownUtils;
    private final Provider<ITreeBuilder> treeBuilderProvider;

	@Inject
    public ListMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IMarkdownUtils markdownUtils,
        Provider<ITreeBuilder> treeBuilderProvider)
	{
		Preconditions.checkNotNull(json);
		Preconditions.checkNotNull(messageFactory);
		Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(treeBuilderProvider);

		this.json = json;
		this.messageFactory = messageFactory;
		this.markdownUtils = markdownUtils;
        this.treeBuilderProvider = treeBuilderProvider;

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
		details.hideAfter = true;

		var optionalRequest = json.deserialize(call.function.arguments, Request.class);
		if (optionalRequest.isEmpty())
		{
			throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
		}

		var request = optionalRequest.get();
		var path = request.path != null && !request.path.isBlank() ? request.path : System.getProperty("user.dir");

		var ignorePatterns = new HashSet<>(IGNORE_PATTERNS);
		if (request.ignore != null && !request.ignore.isEmpty())
		{
			ignorePatterns.addAll(request.ignore);
		}

		if (call.callKind == ToolCallKind.RENDER)
		{
			var pathDisplay = markdownUtils.escapeForMarkdown(path);
			details.requestMarkdown = MessageFormat.format(Messages.ListTitleTemplate, pathDisplay);
			return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
		}

		return CompletableFuture.supplyAsync(() -> {
			if (cancellationToken.isCanceled())
			{
				throw new ToolException("Operation was cancelled before execution.");
			}

			var baseDir = new File(path);
			if (!baseDir.exists() || !baseDir.isDirectory())
			{
				throw new ToolException("The directory \"" + path + "\" does not exist or is not a directory.");
			}

			var result = new Result();
			result.path = path;

			try
			{
				var files = new ArrayList<String>();
				var relevantPaths = new HashSet<String>();
				scanDirectory(baseDir.toPath(), files, relevantPaths, ignorePatterns, 0, LIMIT, cancellationToken);

				result.count = files.size();
				result.truncated = files.size() >= LIMIT;

                ITreeBuilder treeBuilder = treeBuilderProvider.get();
				buildTree(baseDir.toPath(), "", 0, relevantPaths, treeBuilder, ignorePatterns,
					cancellationToken);
				result.tree = treeBuilder.build();
			}
			catch (IOException e)
			{
				throw new ToolException("Directory listing failed", e, ToolErrorType.RETRYABLE);
			}

			var content = json.serialize(result);

			var styledCount = markdownUtils.createStyledText(String.valueOf(result.count), TextColor.GREEN,
				FontWeight.BOLD, false);
			details.responseMarkdown =
				MessageFormat.format(Messages.ListTemplate, markdownUtils.escapeForMarkdown(path), styledCount);
			details.hideAfter = result.count == 0;

			return messageFactory.createMessage(this, call, content, details);
		});
	}

	private void scanDirectory(Path dir, List<String> files, Set<String> relevantPaths, Set<String> ignorePatterns,
		int depth, int limit, ICancellationToken cancellationToken) throws IOException
	{
		if (cancellationToken.isCanceled())
		{
			return;
		}

		try (Stream<Path> stream = Files.list(dir))
		{
			stream.sorted(Comparator.comparing(Path::getFileName)).forEach(path -> {
				if (cancellationToken.isCanceled() || files.size() >= limit)
				{
					return;
				}

				try
				{
					var relativePath = dir.relativize(path).toString();
					var shouldIgnore = shouldIgnore(relativePath, path, ignorePatterns);
					if (shouldIgnore)
					{
						return;
					}

                    if (Files.isDirectory(path))
                    {
                        if (files.size() < limit)
						{
							scanDirectory(path, files, relevantPaths, ignorePatterns, depth + 1, limit, cancellationToken);
                        }

                        if (containsRelevantFiles(path, relevantPaths))
                        {
                            relevantPaths.add(path.toAbsolutePath().toString());
						}
                    }
					else if (Files.isRegularFile(path))
					{
						files.add(relativePath);
						relevantPaths.add(path.toAbsolutePath().toString());
						relevantPaths.add(path.getParent().toAbsolutePath().toString());
					}
				}
				catch (IOException e)
				{
					// Skip unreadable files/directories
				}
			});
		}
	}

    @SuppressWarnings("nls")
    private boolean shouldIgnore(String relativePath, Path path, Set<String> ignorePatterns)
	{
		for (String pattern : ignorePatterns)
		{
			var patternTrimmed = pattern.replace("/", "");
			if (relativePath.equals(patternTrimmed) || relativePath.startsWith(patternTrimmed + "/"))
			{
				return true;
			}
			var patternNormalized = pattern.replace("\\", "/");
			if (relativePath.equals(patternNormalized) || relativePath.startsWith(patternNormalized + "/"))
			{
				return true;
			}
		}
		return false;
	}

	private boolean containsRelevantFiles(Path dir, Set<String> relevantPaths) throws IOException
	{
		try (Stream<Path> stream = Files.list(dir))
		{
			return stream.anyMatch(path -> relevantPaths.contains(path.toAbsolutePath().toString()));
		}
	}

    @SuppressWarnings("nls")
    private void buildTree(Path dir, String relativePath, int depth, Set<String> relevantPaths,
        ITreeBuilder treeBuilder, Set<String> ignorePatterns, ICancellationToken cancellationToken)
			throws IOException
	{
		if (cancellationToken.isCanceled())
		{
			return;
		}

		try (Stream<Path> stream = Files.list(dir))
		{
			stream.sorted(Comparator.comparing(Path::getFileName)).forEach(path -> {
				if (cancellationToken.isCanceled())
				{
					return;
				}

				try
				{
					var currentRelativePath = relativePath.isEmpty() ? path.getFileName().toString()
						: relativePath + "/" + path.getFileName().toString();

					var absolutePath = path.toAbsolutePath().toString();
					var isRelevant = relevantPaths.contains(absolutePath);
					var shouldIgnore = shouldIgnore(currentRelativePath, path, ignorePatterns);

					if (shouldIgnore)
					{
						return;
					}

					if (Files.isDirectory(path) && isRelevant)
					{
						treeBuilder.addDirectory(path.getFileName().toString(), depth);
						buildTree(path, currentRelativePath, depth + 1, relevantPaths, treeBuilder, ignorePatterns,
							cancellationToken);
						treeBuilder.endDirectory();
					}
					else if (Files.isRegularFile(path) && isRelevant)
					{
						treeBuilder.addFile(path.getFileName().toString(), depth);
					}
				}
				catch (IOException e)
				{
					// Skip unreadable files/directories
				}
			});
		}
	}

	@SuppressWarnings("nls")
	private static McpToolCallSpecification createSpecification()
	{
		var spec = new McpToolCallSpecification();
		spec.type = "function";
		spec.function = new McpToolCallFunction();
		spec.function.name = TOOL_NAME;

		var description = new StringBuilder();
		description.append("Lists directory contents in a tree structure.");
		description.append("\n\nUsage:");
		description.append("\n- Arguments must be a single JSON object.");
		description.append("\n- Lists all files and directories in a tree format.");
		description.append("\n- Automatically ignores common build/cache directories: node_modules, .git, dist, build, etc.");
		description.append("\n- Optionally specify custom ignore patterns.");
		description.append("\n- Limited to " + LIMIT + " files for performance.");
		description.append("\n- Use this tool to explore directory structure quickly.");
		description.append("\n\nExample:");
		description.append("\n  Q: "); description.append(QuestionExample);
		description.append("\n  A: "); description.append(AnswerExample);

		spec.function.description = description.toString();

		var parameters = new McpToolCallParameters();
		parameters.type = "object";
		var properties = new HashMap<String, McpToolCallProperty>();

		var pathProp = new McpToolCallProperty();
		pathProp.type = "string";
		pathProp.description =
			"The absolute path to the directory to list. If not specified, the current working directory will be used.";
		properties.put("path", pathProp);

		var ignoreProp = new McpToolCallProperty();
		ignoreProp.type = "array";
		ignoreProp.description =
			"List of glob patterns to ignore (e.g., [\"*.tmp\", \"temp/\"]). Default patterns are always included.";
		properties.put("ignore", ignoreProp);

		parameters.properties = properties;
		parameters.required = Arrays.asList();
		spec.function.parameters = parameters;

		return spec;
	}

	private static class Request
	{
		@SerializedName("path")
		public String path;

		@SerializedName("ignore")
		public List<String> ignore;
	}

	private static class Result
	{
		@SerializedName("path")
		public String path;

		@SerializedName("count")
		public int count;

		@SerializedName("truncated")
		public boolean truncated;

		// Large field last so it is dropped first if the response is truncated.
		@SerializedName("tree")
		public String tree;
	}
}
