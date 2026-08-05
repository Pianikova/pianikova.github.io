/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
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
    static final List<String> IGNORE_PATTERNS = Arrays.asList(
		"node_modules/",
		"__pycache__/",
		".git/",
		"bin/",
		"obj/",
		".zig-cache/",
		".coverage",
		"tmp/",
		"temp/",
		".cache/",
        "cache/"
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
    private final IPatternMatcher patternMatcher;

	@Inject
    public ListMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IMarkdownUtils markdownUtils,
        Provider<ITreeBuilder> treeBuilderProvider, IPatternMatcher patternMatcher)
	{
		Preconditions.checkNotNull(json);
		Preconditions.checkNotNull(messageFactory);
		Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(treeBuilderProvider);
        Preconditions.checkNotNull(patternMatcher);

		this.json = json;
		this.messageFactory = messageFactory;
		this.markdownUtils = markdownUtils;
        this.treeBuilderProvider = treeBuilderProvider;
        this.patternMatcher = patternMatcher;

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
		var pattern = request.pattern != null && !request.pattern.isBlank() ? request.pattern : "*";

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
			result.items = new ArrayList<>();

			try
			{
				var relevantPaths = new HashSet<String>();
				scanDirectory(baseDir.toPath(), baseDir.toPath(), pattern, ignorePatterns, result, relevantPaths,
					cancellationToken, LIMIT);

				result.count = result.items.size();
				result.truncated = result.items.size() >= LIMIT;
				result.items.sort((a, b) -> Long.compare(b.modified, a.modified));

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

    @SuppressWarnings("nls")
    private void scanDirectory(Path baseDir, Path dir, String pattern, Set<String> ignorePatterns, Result result,
        Set<String> relevantPaths, ICancellationToken cancellationToken, int limit) throws IOException
	{
		if (cancellationToken.isCanceled() || result.items.size() >= limit)
		{
			return;
		}

		try (Stream<Path> stream = Files.list(dir))
		{
			stream.sorted(Comparator.comparing(Path::getFileName)).forEach(path -> {
				if (cancellationToken.isCanceled() || result.items.size() >= limit)
				{
					return;
				}

				try
				{
					var relativePath = dir.relativize(path).toString();
					if (shouldIgnore(relativePath, path, ignorePatterns))
					{
						return;
					}

					var attrs = Files.readAttributes(path, BasicFileAttributes.class);
					var fullRelativePath = baseDir.relativize(path).toString().replace("\\", "/");
					var matchesPattern = patternMatcher.matches(fullRelativePath, pattern);

					if (Files.isDirectory(path))
					{
						if (matchesPattern)
						{
							var dirInfo = new ItemInfo();
							dirInfo.path = path.toAbsolutePath().toString();
							dirInfo.type = "directory";
							dirInfo.modified = attrs.lastModifiedTime().toMillis();
							result.items.add(dirInfo);
							relevantPaths.add(path.toAbsolutePath().toString());
						}

						var beforeCount = relevantPaths.size();
						scanDirectory(baseDir, path, pattern, ignorePatterns, result, relevantPaths,
							cancellationToken, limit);
						if (relevantPaths.size() > beforeCount)
						{
							relevantPaths.add(dir.toAbsolutePath().toString());
						}
					}
					else if (Files.isRegularFile(path) && matchesPattern)
					{
						var fileInfo = new ItemInfo();
						fileInfo.path = path.toAbsolutePath().toString();
						fileInfo.type = "file";
						fileInfo.modified = attrs.lastModifiedTime().toMillis();
						result.items.add(fileInfo);
						relevantPaths.add(path.toAbsolutePath().toString());
						relevantPaths.add(dir.toAbsolutePath().toString());
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
		description.append("\n- Lists all files and directories in a tree format, optionally filtered by `pattern`.");
		description.append("\n- `pattern` uses the same glob syntax as `" + GlobMcpTool.TOOL_NAME + "`: a pattern without \"/\" matches the name at any depth; a pattern with \"/\" is anchored to the root unless it contains \"**\". Defaults to \"*\" (everything).");
		description.append("\n- Unlike `" + GlobMcpTool.TOOL_NAME + "`, there is no depth limit: the whole tree under `path` is scanned.");
        description
            .append("\n- Automatically ignores common build/cache directories: node_modules, .git, bin, obj, etc.");
		description.append("\n- Optionally specify custom ignore patterns via `ignore`.");
		description.append("\n- Limited to " + LIMIT + " matched items for performance. If the response has \"truncated\": true, more than " + LIMIT + " items were found; use `" + GlobMcpTool.TOOL_NAME + "` or `" + SearchTextMcpTool.TOOL_NAME + "` with a narrower pattern or pagination instead of retrying `" + TOOL_NAME + "`.");
		description.append("\n- Response includes `items` (each: `path` absolute, `type` file/directory, `modified` timestamp, sorted newest first) in addition to the rendered `tree`.");
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

		var patternProp = new McpToolCallProperty();
		patternProp.type = "string";
		patternProp.description =
			"Glob pattern to filter files/directories (same syntax as " + GlobMcpTool.TOOL_NAME + ": '*', '?', '**'). Defaults to \"*\" (everything, subject to the default ignore list).";
		properties.put("pattern", patternProp);

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

		@SerializedName("pattern")
		public String pattern;

		@SerializedName("ignore")
		public List<String> ignore;
	}

	private static class ItemInfo
	{
		@SerializedName("path")
		public String path;

		@SerializedName("type")
		public String type;

		@SerializedName("modified")
		public long modified;
	}

	private static class Result
	{
		@SerializedName("path")
		public String path;

		@SerializedName("count")
		public int count;

		@SerializedName("truncated")
		public boolean truncated;

		// Large fields last so they are dropped first if the response is truncated.
		@SerializedName("tree")
		public String tree;

		@SerializedName("items")
		public List<ItemInfo> items;
	}
}
