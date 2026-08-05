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

public class GlobMcpTool
	implements IMcpTool
{
	public static final String TOOL_NAME = "Glob"; //$NON-NLS-1$
    private static final int LIMIT = 100;

	@SuppressWarnings("nls")
	private static String QuestionExample =
		"{\n"
		+ "  \"path\": \"/home/user/workspace\",\n"
            + "  \"pattern\": \"src/**/*.bsl\",\n" + "  \"depth\": 3\n"
		+ "}\n\n"
		+ "// List current directory contents (default):\n"
		+ "{\n"
		+ "  \"pattern\": \"*\"\n"
            + "}\n\n" + "// Find all Java files in any subdirectory:\n" + "{\n" + "  \"pattern\": \"**/*.java\"\n"
		+ "}";

	private final IJson json;
	private final McpToolCallSpecification spec;
	private final IMcpToolsCallMessageFactory messageFactory;
	private final IMarkdownUtils markdownUtils;
    private final IPatternMatcher patternMatcher;
    private final Provider<ITreeBuilder> treeBuilderProvider;

	@Inject
    public GlobMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory, IMarkdownUtils markdownUtils,
        IPatternMatcher patternMatcher, Provider<ITreeBuilder> treeBuilderProvider)
	{
		Preconditions.checkNotNull(json);
		Preconditions.checkNotNull(messageFactory);
		Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(patternMatcher);
        Preconditions.checkNotNull(treeBuilderProvider);
		this.json = json;
		this.messageFactory = messageFactory;
		this.markdownUtils = markdownUtils;
        this.patternMatcher = patternMatcher;
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
        var path = request.path;
		if (path == null || path.isBlank())
		{
			throw new ToolException("The \"path\" parameter is required and must be a valid directory path.");
		}
		var pattern = request.pattern != null ? request.pattern : "*";
        var userDepth = request.depth != null ? request.depth : 3;
        // Increase depth for patterns with ** to support deeper searches
        var depth = (pattern.contains("**") && userDepth < 10) ? 10 : userDepth;

        var ignorePatterns = new HashSet<>(ListMcpTool.IGNORE_PATTERNS);
        if (request.ignore != null && !request.ignore.isEmpty())
        {
            ignorePatterns.addAll(request.ignore);
        }

		if (call.callKind == ToolCallKind.RENDER)
		{
            var patternDisplay = request.pattern != null ? markdownUtils.escapeForMarkdown(request.pattern)
                : markdownUtils.escapeForMarkdown("*");
            details.requestMarkdown = MessageFormat.format(Messages.GlobTitleTemplate, patternDisplay);
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
			result.items = new ArrayList<>();
			result.stats = new Stats();

		try
		{
                var relevantPaths = new HashSet<String>();
                ITreeBuilder treeBuilder = treeBuilderProvider.get();
                scanDirectory(baseDir.toPath(), baseDir.toPath(), pattern, ignorePatterns, depth, 0, result, relevantPaths, cancellationToken, LIMIT);
                if (!relevantPaths.isEmpty())
                {
                    relevantPaths.add(baseDir.getAbsolutePath());
                }
		scanDirectoryForTree(baseDir.toPath(), baseDir.toPath(), pattern, ignorePatterns, depth, 0, result, relevantPaths, treeBuilder, cancellationToken);
		result.tree = treeBuilder.build();

                result.items.sort((a, b) -> Long.compare(b.modified, a.modified));
                result.stats.truncated = result.items.size() >= LIMIT;
		}
			catch (IOException e)
			{
				throw new ToolException("Directory listing failed", e, ToolErrorType.RETRYABLE);
			}

			var content = json.serialize(result);

            var styledItemsCount = markdownUtils.createStyledText(String.valueOf(result.stats.totalItems),
                TextColor.GREEN, FontWeight.BOLD, false);
            details.responseMarkdown =
                MessageFormat.format(Messages.GlobTemplate, markdownUtils.escapeForMarkdown(pattern), styledItemsCount);
			details.hideAfter = result.stats.totalItems == 0;

			return messageFactory.createMessage(this, call, content, details);
		});
	}

    @SuppressWarnings("nls")
    private void scanDirectory(Path baseDir, Path dir, String pattern, Set<String> ignorePatterns, int maxDepth,
        int currentDepth, Result result, Set<String> relevantPaths, ICancellationToken cancellationToken, int limit)
        throws IOException
	{
        if (cancellationToken.isCanceled() || currentDepth > maxDepth || result.items.size() >= limit)
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
					if (shouldIgnore(dir.relativize(path).toString(), ignorePatterns))
					{
						return;
					}

					var attrs = Files.readAttributes(path, BasicFileAttributes.class);
					var relativePath = baseDir.relativize(path).toString();
					// Normalize path separators to forward slashes for pattern matching
					var normalizedPath = relativePath.replace("\\", "/");
					var matchesPattern = patternMatcher.matches(normalizedPath, pattern);

					if (Files.isDirectory(path))
					{
                        if (matchesPattern)
                        {
   						var dirInfo = new ItemInfo();
   						dirInfo.path = path.toAbsolutePath().toString();
   						dirInfo.type = "directory";
   						dirInfo.modified = attrs.lastModifiedTime().toMillis();
   						result.items.add(dirInfo);
   						result.stats.totalItems++;
                            relevantPaths.add(path.toAbsolutePath().toString());
                        }
                        var beforeCount = relevantPaths.size();
                        scanDirectory(baseDir, path, pattern, ignorePatterns, maxDepth, currentDepth + 1, result,
                            relevantPaths, cancellationToken, limit);
                        var afterCount = relevantPaths.size();

                        if (afterCount > beforeCount)
                        {
                            relevantPaths.add(dir.toAbsolutePath().toString());
                        }

                        if (afterCount > beforeCount || matchesPattern)
                        {
                            result.stats.maxDepthReached = Math.max(result.stats.maxDepthReached, currentDepth + 1);
                        }
					}
					else if (Files.isRegularFile(path))
					{
                        if (matchesPattern)
						{
							var fileInfo = new ItemInfo();
							fileInfo.path = path.toAbsolutePath().toString();
							fileInfo.type = "file";
							fileInfo.modified = attrs.lastModifiedTime().toMillis();
							result.items.add(fileInfo);
							result.stats.totalItems++;
                            relevantPaths.add(path.toAbsolutePath().toString());
                            relevantPaths.add(dir.toAbsolutePath().toString());
                            result.stats.maxDepthReached = Math.max(result.stats.maxDepthReached, currentDepth);
						}
					}
				}
				catch (IOException e)
				{
                    //
				}
			});
		}
	}

    @SuppressWarnings("nls")
    private void scanDirectoryForTree(Path baseDir, Path dir, String pattern, Set<String> ignorePatterns,
        int maxDepth, int currentDepth, Result result, Set<String> relevantPaths, ITreeBuilder treeBuilder,
        ICancellationToken cancellationToken) throws IOException
	{
		if (cancellationToken.isCanceled() || currentDepth > maxDepth)
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
					if (shouldIgnore(dir.relativize(path).toString(), ignorePatterns))
					{
						return;
					}

					var relativePath = baseDir.relativize(path).toString();
					// Normalize path separators to forward slashes for tree display
					var normalizedPath = relativePath.replace("\\", "/");
					var absolutePath = path.toAbsolutePath().toString();
					var isRelevant = relevantPaths.contains(absolutePath);

				if (Files.isDirectory(path))
				{
                        if (isRelevant)
                        {
                            treeBuilder.addDirectory(normalizedPath, currentDepth);
                            scanDirectoryForTree(baseDir, path, pattern, ignorePatterns, maxDepth, currentDepth + 1,
                                result, relevantPaths, treeBuilder, cancellationToken);
                            treeBuilder.endDirectory();
                        }
				}
				else if (Files.isRegularFile(path))
				{
                        if (isRelevant)
				{
					treeBuilder.addFile(normalizedPath, currentDepth);
				}
				}
				}
				catch (IOException e)
				{
                    //
				}
			});
		}
}

    @SuppressWarnings("nls")
    private static boolean shouldIgnore(String relativePath, Set<String> ignorePatterns)
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
	private static McpToolCallSpecification createSpecification()
	{
		var spec = new McpToolCallSpecification();
		spec.type = "function";
		spec.function = new McpToolCallFunction();
		spec.function.name = TOOL_NAME;

		var description = new StringBuilder();
		description.append("Fast directory listing tool that works with any configuration size.");
		description.append("\n\nUsage:");
		description.append("\n- Arguments must be a single JSON object.");
		description.append("\n- Lists directory contents with configurable depth and optional pattern matching.");
		description.append("\n- By default uses pattern=\"*\" and depth=3, listing names down to three levels. Set depth=0 for an ls-like listing of the root only.");
        description.append("\n- Supports glob patterns with path separators (\"/\" or \"\\\\\"):");
        description.append("\n  - A pattern WITHOUT \"/\" is matched against the file/directory name at ANY depth:");
        description.append("\n    - \"*.bsl\" - all .bsl files at any depth");
        description.append("\n    - \"*Module*\" - any name containing \"Module\" at any depth (e.g. src/Catalogs/Контрагенты/ManagerModule.bsl)");
        description.append("\n  - A pattern WITH \"/\" is anchored to the search root and matched segment by segment:");
        description.append("\n    - \"src/*.bsl\" - .bsl files directly under src/ only");
        description.append("\n    - \"src/**/*.bsl\" - .bsl files anywhere under src/");
        description.append("\n    - \"**/*.java\" - .java files in any subdirectory");
        description.append("\n    - \"**/test_*.py\" - test_*.py files in any subdirectory");
        description.append("\n- Wildcards: \"*\" (any characters), \"?\" (single character), \"**\" (any number of directory segments, including zero).");
        description.append("\n- Automatically ignores common build/cache directories: node_modules, .git, bin, obj etc. Optionally add more via `ignore`.");
		description.append("\n- Returns matching file and directory paths sorted by modification time.");
		description.append("\n- Use this tool when you need to explore directory structure or list files.");
		description.append("\n- Depth parameter controls how deep to traverse subdirectories (0 = only root, 1 = root + one level, etc.).");
		spec.function.description = description.toString();

		var parameters = new McpToolCallParameters();
		parameters.type = "object";
		var properties = new HashMap<String, McpToolCallProperty>();

		var pathProp = new McpToolCallProperty();
		pathProp.type = "string";
		pathProp.description = "The directory to search in. Must be a valid directory path.";
		properties.put("path", pathProp);

		var patternProp = new McpToolCallProperty();
		patternProp.type = "string";
        patternProp.description =
            "The search pattern to match files or directories. A pattern without \"/\" matches the name at any depth (\"*.bsl\", \"*Module*\"); a pattern with \"/\" is anchored to the search root (\"src/*.bsl\", \"src/**/*.bsl\", \"**/*.java\", \"**/test_*.py\"). Wildcards: '*' (any characters), '?' (single character), '**' (any number of directory segments). If omitted, all files and directories are matched. Default: \"*\"";
		properties.put("pattern", patternProp);

		var depthProp = new McpToolCallProperty();
		depthProp.type = "integer";
        depthProp.description =
            "The maximum depth of subdirectories to search. A value of 0 means only the root directory, 1 includes one level of subdirectories, etc. Defaults to 3 if not specified.";
		properties.put("depth", depthProp);

		var ignoreProp = new McpToolCallProperty();
		ignoreProp.type = "array";
		ignoreProp.description =
			"List of glob patterns to ignore (e.g., [\"*.tmp\", \"temp/\"]). Default patterns (node_modules, .git, etc.) are always included.";
		properties.put("ignore", ignoreProp);

		parameters.properties = properties;
		parameters.required = Arrays.asList("path");
		spec.function.parameters = parameters;

		return spec;
	}

	private static class Request
	{
		@SerializedName("path")
		public String path;

		@SerializedName("pattern")
		public String pattern;

		@SerializedName("depth")
		public Integer depth;

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

	private static class Stats
	{
		@SerializedName("total_items")
		public int totalItems;

		@SerializedName("max_depth_reached")
		public int maxDepthReached;

		@SerializedName("truncated")
		public boolean truncated;
	}

	private static class Result
	{
		@SerializedName("stats")
		public Stats stats;

		// Large fields last so they are dropped first if the response is truncated.
		@SerializedName("tree")
		public String tree;

		@SerializedName("items")
		public List<ItemInfo> items;
	}
}
