/**
* Copyright (C) 2025, 1C
*/
package com.e1c.edt.ai.tools;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;

import com.e1c.edt.ai.FontWeight;
import com.e1c.edt.ai.ICancellationProgressMonitor;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IEditingSupport;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IMcpToolsCallMessageFactory;
import com.e1c.edt.ai.IProjectTools;
import com.e1c.edt.ai.IReadOnlyProjectGuard;
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
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Provider;

/**
 * Lets the model author SVG markup itself, then validates it (well-formedness with a line and
 * column for self-correction), removes unsafe constructs, saves it as a file and shows the
 * rendered picture inline in the chat. Pure local validation - no external API, no rasterizer.
 */
public class SvgMcpTool
    implements IMcpTool
{
    public static final String TOOL_NAME = "Svg"; //$NON-NLS-1$

    private static final String ACTION_SAVE = "save"; //$NON-NLS-1$

    private static final String ACTION_CHECK = "check"; //$NON-NLS-1$

    private static final String ACTION_PREVIEW = "preview"; //$NON-NLS-1$

    // @formatter:off
    @SuppressWarnings("nls")
    private static String QuestionExample =
        "{\n"
        + "  \"action\": \"save\",\n"
        + "  \"path\": \"C:/Projects/AccountingSystem/docs/posting-flow.svg\",\n"
        + "  \"title\": \"Document posting flow\",\n"
        + "  \"content\": \"<svg xmlns=\\\"http://www.w3.org/2000/svg\\\" viewBox=\\\"0 0 240 120\\\">"
        + "<rect x=\\\"10\\\" y=\\\"35\\\" width=\\\"90\\\" height=\\\"50\\\" rx=\\\"6\\\" "
        + "fill=\\\"#e8f0fe\\\" stroke=\\\"#1a73e8\\\"/>"
        + "<text x=\\\"55\\\" y=\\\"64\\\" text-anchor=\\\"middle\\\" font-size=\\\"12\\\">Document</text>"
        + "<path d=\\\"M105 60 H145\\\" stroke=\\\"#5f6368\\\" stroke-width=\\\"2\\\"/>"
        + "</svg>\"\n"
        + "}";
    @SuppressWarnings("nls")
    private static String AnswerExample =
        "SVG saved: \"C:/Projects/AccountingSystem/docs/posting-flow.svg\" "
        + "(0.3 KB, 4 elements, viewBox \"0 0 240 120\"). Preview is shown in the chat.";
    // @formatter:on

    private final IJson json;
    private final McpToolCallSpecification spec;
    private final IMcpToolsCallMessageFactory messageFactory;
    private final Provider<ICancellationProgressMonitor> cancellationProgressMonitor;
    private final IFileSystem fileSystem;
    private final IProjectTools projectTools;
    private final IMarkdownUtils markdownUtils;
    private final IEditingSupport editingSupport;
    private final IReadOnlyProjectGuard readOnlyProjectGuard;
    private final ISvgSanitizer sanitizer;
    private final ILog log;

    @Inject
    public SvgMcpTool(IJson json, IMcpToolsCallMessageFactory messageFactory,
        Provider<ICancellationProgressMonitor> cancellationProgressMonitor, IFileSystem fileSystem,
        IProjectTools projectTools, IMarkdownUtils markdownUtils, IEditingSupport editingSupport,
        IReadOnlyProjectGuard readOnlyProjectGuard, ISvgSanitizer sanitizer, ILog log)
    {
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(messageFactory);
        Preconditions.checkNotNull(cancellationProgressMonitor);
        Preconditions.checkNotNull(fileSystem);
        Preconditions.checkNotNull(projectTools);
        Preconditions.checkNotNull(markdownUtils);
        Preconditions.checkNotNull(editingSupport);
        Preconditions.checkNotNull(readOnlyProjectGuard);
        Preconditions.checkNotNull(sanitizer);
        Preconditions.checkNotNull(log);

        this.json = json;
        this.messageFactory = messageFactory;
        this.cancellationProgressMonitor = cancellationProgressMonitor;
        this.fileSystem = fileSystem;
        this.projectTools = projectTools;
        this.markdownUtils = markdownUtils;
        this.editingSupport = editingSupport;
        this.readOnlyProjectGuard = readOnlyProjectGuard;
        this.sanitizer = sanitizer;
        this.log = log;

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
        details.autoCall = false;

        var optionalRequest = json.deserialize(call.function.arguments, Request.class);
        if (optionalRequest.isEmpty())
        {
            throw new ToolException("Cannot deserialize arguments. Use this example: " + QuestionExample);
        }
        var request = optionalRequest.get();

        var path = request.path;
        if (path == null || path.isBlank())
        {
            throw new ToolException("`path` is required.");
        }
        if (!path.toLowerCase(Locale.ROOT).endsWith(".svg"))
        {
            throw new ToolException("`path` must end with \".svg\".");
        }

        var action = normalizeAction(request.action);
        if ((ACTION_SAVE.equals(action) || ACTION_CHECK.equals(action))
            && (request.content == null || request.content.isBlank()))
        {
            throw new ToolException("`content` is required for action \"" + action + "\".");
        }

        if (call.callKind == ToolCallKind.RENDER)
        {
            // Best-effort: render the picture the model is proposing right in the confirmation
            // card, so the user judges it visually instead of reading raw markup/diff text.
            // RENDER must never write to disk or otherwise mutate state.
            String requestMarkdown;
            if (ACTION_SAVE.equals(action))
            {
                var headerHtml =
                    MessageFormat.format(Messages.SvgSaveTitleTemplate, markdownUtils.formatFilePath(path));
                requestMarkdown = SvgReport.buildResponseMarkdown(headerHtml, sanitizer.sanitize(request.content),
                    request.title);
            }
            else if (ACTION_CHECK.equals(action))
            {
                requestMarkdown = buildCheckResponseMarkdown(sanitizer.sanitize(request.content), request.title);
            }
            else
            {
                var headerHtml =
                    MessageFormat.format(Messages.SvgPreviewTitleTemplate, markdownUtils.formatFilePath(path));
                String source = tryReadFile(path);
                requestMarkdown = source != null
                    ? SvgReport.buildResponseMarkdown(headerHtml, sanitizer.sanitize(source), request.title)
                    : headerHtml;
            }
            details.requestMarkdown = requestMarkdown;
            return CompletableFuture.completedFuture(messageFactory.createMessage(this, call, null, details));
        }

        if (ACTION_CHECK.equals(action))
        {
            return handleCheck(call, cancellationToken, details, request);
        }
        if (ACTION_PREVIEW.equals(action))
        {
            return handlePreview(call, cancellationToken, details, request, path);
        }
        return handleSave(call, cancellationToken, details, request, path);
    }

    /**
     * Best-effort read of the file at {@code path} for RENDER preview. Returns {@code null} on any
     * failure (missing file, I/O error) — RENDER must remain side-effect-free and resilient.
     */
    private String tryReadFile(String path)
    {
        try
        {
            if (!fileSystem.fileExists(path))
            {
                return null;
            }
            return new String(fileSystem.readAllBytes(path), StandardCharsets.UTF_8);
        }
        catch (IOException error)
        {
            return null;
        }
    }

    @SuppressWarnings("nls")
    private static String normalizeAction(String raw)
    {
        if (raw == null || raw.isBlank())
        {
            return ACTION_SAVE;
        }
        var action = raw.trim().toLowerCase(Locale.ROOT);
        if (!ACTION_SAVE.equals(action) && !ACTION_CHECK.equals(action) && !ACTION_PREVIEW.equals(action))
        {
            throw new ToolException("`action` must be one of \"save\", \"check\", \"preview\".");
        }
        return action;
    }

    @SuppressWarnings("nls")
    private CompletableFuture<ToolCallMessage> handleCheck(McpToolCall call, ICancellationToken cancellationToken,
        ToolCallMessageDetails details, Request request)
    {
        return CompletableFuture.supplyAsync(() -> {
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation was cancelled before execution.");
            }

            var result = sanitizer.sanitize(request.content);
            var content = SvgReport.buildCheckContent(result);
            details.responseMarkdown = buildCheckResponseMarkdown(result, request.title);
            return messageFactory.createMessage(this, call, content, details);
        });
    }

    @SuppressWarnings("nls")
    private CompletableFuture<ToolCallMessage> handlePreview(McpToolCall call, ICancellationToken cancellationToken,
        ToolCallMessageDetails details, Request request, String path)
    {
        return CompletableFuture.supplyAsync(() -> {
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation was cancelled before execution.");
            }

            boolean exists;
            try
            {
                exists = fileSystem.fileExists(path);
            }
            catch (IOException error)
            {
                throw new ToolException("Failed to read file \"" + path + "\"", error, ToolErrorType.RETRYABLE);
            }
            if (!exists)
            {
                throw new ToolException("The file \"" + path + "\" does not exist.", ToolErrorType.USER_VISIBLE);
            }

            String source;
            try
            {
                source = new String(fileSystem.readAllBytes(path), StandardCharsets.UTF_8);
            }
            catch (IOException error)
            {
                throw new ToolException("Failed to read file \"" + path + "\"", error, ToolErrorType.RETRYABLE);
            }

            var result = sanitizer.sanitize(source);
            var content = SvgReport.buildPreviewContent(path, result);
            details.responseMarkdown = buildPreviewResponseMarkdown(path, result, request.title);
            return messageFactory.createMessage(this, call, content, details);
        });
    }

    @SuppressWarnings("nls")
    private CompletableFuture<ToolCallMessage> handleSave(McpToolCall call, ICancellationToken cancellationToken,
        ToolCallMessageDetails details, Request request, String path)
    {
        return CompletableFuture.supplyAsync(() -> {
            if (cancellationToken.isCanceled())
            {
                throw new ToolException("Operation was cancelled before execution.");
            }

            var result = sanitizer.sanitize(request.content);
            if (!result.isValid())
            {
                throw new ToolException(SvgReport.buildSaveContent(path, result, false), ToolErrorType.RETRYABLE);
            }

            boolean overwrite = Boolean.TRUE.equals(request.overwrite);
            byte[] data = result.getSanitizedSource().getBytes(StandardCharsets.UTF_8);

            String detectedProjectName = projectTools.determineProjectName(path);
            boolean isProjectFile = detectedProjectName != null && !detectedProjectName.isBlank();

            if (isProjectFile)
            {
                var root = ResourcesPlugin.getWorkspace().getRoot();
                var project = root.getProject(detectedProjectName);
                if (project == null || !project.exists())
                {
                    throw new ToolException("The project \"" + detectedProjectName + "\" does not exist.");
                }

                var monitor = cancellationProgressMonitor.get();
                monitor.setCancellationToken(cancellationToken);
                if (!project.isOpen())
                {
                    try
                    {
                        project.open(monitor);
                    }
                    catch (CoreException error)
                    {
                        throw new ToolException("Cannot open the project \"" + detectedProjectName + "\"", error,
                            ToolErrorType.RETRYABLE);
                    }
                }

                readOnlyProjectGuard.checkWritable(project);

                var optionalProjectFile = projectTools.getProjectFile(project, path);
                if (optionalProjectFile.isPresent())
                {
                    var projectFile = optionalProjectFile.get();
                    boolean exists = projectFile.exists();
                    boolean nonEmpty = false;
                    if (exists)
                    {
                        try
                        {
                            nonEmpty =
                                projectFile.getLocation() != null && projectFile.getLocation().toFile().length() > 0;
                        }
                        catch (Exception error)
                        {
                            nonEmpty = true;
                        }
                    }

                    if (exists && nonEmpty && !overwrite)
                    {
                        throw new ToolException("The file \"" + path
                            + "\" already exists and is not empty. Pass `overwrite: true` to replace it.");
                    }

                    boolean permitted =
                        exists ? editingSupport.canEdit(projectFile) : editingSupport.canCreate(projectFile);
                    if (!permitted)
                    {
                        throw new ToolException("The file \"" + path
                            + "\" cannot be written. Writing is not supported for this file type or the location is restricted.");
                    }

                    try
                    {
                        WorkspaceFileWriter.createParentFolders(projectFile, monitor);
                        try (ByteArrayInputStream source = new ByteArrayInputStream(data))
                        {
                            if (exists)
                            {
                                projectFile.setContents(source, IResource.FORCE, monitor);
                            }
                            else
                            {
                                projectFile.create(source, true, monitor);
                            }
                            WorkspaceFileWriter.refreshResources(projectFile, monitor);
                        }
                    }
                    catch (CoreException | IOException error)
                    {
                        WorkspaceFileWriter.refreshResourcesSafe(projectFile, monitor, log);
                        throw new ToolException("Failed to write file", error, ToolErrorType.RETRYABLE);
                    }

                    var content = SvgReport.buildSaveContent(path, result, false);
                    details.responseMarkdown = buildSaveResponseMarkdown(path, result, request.title);
                    return messageFactory.createMessage(this, call, content, details);
                }
            }

            // File is not part of any project - use Java file I/O, as WriteMcpTool does.
            try
            {
                boolean exists = fileSystem.fileExists(path);
                boolean nonEmpty = exists && !fileSystem.isFileEmpty(path);
                if (exists && nonEmpty && !overwrite)
                {
                    throw new ToolException("The file \"" + path
                        + "\" already exists and is not empty. Pass `overwrite: true` to replace it.");
                }

                fileSystem.writeAllBytes(path, data);

                var content = SvgReport.buildSaveContent(path, result, true);
                details.responseMarkdown = buildSaveResponseMarkdown(path, result, request.title);
                return messageFactory.createMessage(this, call, content, details);
            }
            catch (IOException error)
            {
                throw new ToolException("Failed to write file", error, ToolErrorType.RETRYABLE);
            }
        });
    }

    @SuppressWarnings("nls")
    private String buildSaveResponseMarkdown(String path, SvgSanitizeResult result, String title)
    {
        var sizeBadge = SvgReport.formatSize(result.getMetadata().getSizeBytes());
        var sizeHtml = markdownUtils.createStyledText(sizeBadge, TextColor.GREEN, FontWeight.BOLD, false);
        var headerHtml = MessageFormat.format(Messages.SvgSavedTemplate, markdownUtils.formatFilePath(path), sizeHtml);
        return SvgReport.buildResponseMarkdown(headerHtml, result, title);
    }

    private String buildCheckResponseMarkdown(SvgSanitizeResult result, String title)
    {
        var headerHtml = markdownUtils.createStyledText(Messages.SvgCheckTitle, TextColor.GRAY, FontWeight.BOLD, false);
        return SvgReport.buildResponseMarkdown(headerHtml, result, title);
    }

    private String buildPreviewResponseMarkdown(String path, SvgSanitizeResult result, String title)
    {
        var headerHtml = markdownUtils.formatFilePath(path);
        return SvgReport.buildResponseMarkdown(headerHtml, result, title);
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
        description.append("Creates, validates and previews SVG images. You author the SVG markup yourself; this tool checks that it is well-formed, removes unsafe constructs, saves it as a file and shows the rendered picture to the user inline in the chat.");
        description.append("\n\nUsage:");
        description.append("\n- Arguments must be a single JSON object.");
        description.append("\n- `action` selects the mode: \"save\" (default) validates `content` and writes it to `path`; \"check\" validates `content` without touching the disk; \"preview\" renders the SVG that already exists at `path`.");
        description.append("\n- Write self-contained SVG 1.1/2.0 markup whose root element is `<svg>`.");
        description.append("\n- ALWAYS set `viewBox` (for example `viewBox=\"0 0 640 400\"`) and prefer it over fixed `width`/`height`, so the picture scales to the chat panel and to the user's editor.");
        description.append("\n- The markup must be self-contained: no `<script>`, no `on*` event attributes, no `<foreignObject>`, no `<style>` with `@import`, and no references to external files or URLs (`http:`, `https:`, `file:`, relative paths). Only in-document references such as `href=\"#gradient1\"` or `fill=\"url(#gradient1)\"`, and inline raster data such as `data:image/png;base64,...`, are allowed. Anything else is removed automatically and listed back to you.");
        description.append("\n- Use `href`, not `xlink:href`.");
        description.append("\n- Use numeric character references such as `&#8594;`. HTML entity names such as `&nbsp;` or `&rarr;` are not valid XML and will fail to parse.");
        description.append("\n- Keep a drawing under " + (McpToolConstants.SVG_RECOMMENDED_MAX_BYTES / 1024) + " KB. Hard limits: " + McpToolConstants.SVG_MAX_SOURCE_CHARS + " characters of markup and " + McpToolConstants.SVG_MAX_ELEMENTS + " elements. Drawings larger than " + (McpToolConstants.SVG_MAX_PREVIEW_BYTES / 1024) + " KB are saved but not previewed inline.");
        description.append("\n- `path` must be absolute and end with `.svg`. Overwriting an existing non-empty file requires `overwrite: true`.");
        description.append("\n- When the markup is not well-formed the error names the line and column and quotes the offending line. Fix it and call the tool again.");
        description.append("\n- Prefer `check` while iterating on complex artwork, then `save` once.");
        description.append("\n- Avoid emojis in the drawing unless explicitly requested.");
        description.append("\n\nRelated tools:");
        description.append("\n- Any other file type: `" + WriteMcpTool.TOOL_NAME + "`.");
        description.append("\n- Modify an existing SVG in place: `" + EditMcpTool.TOOL_NAME + "`, then call `" + TOOL_NAME + "` with action \"preview\" to show the result.");
        description.append("\n- Inspect the markup of an existing file: `" + ReadMcpTool.TOOL_NAME + "`.");
        description.append("\n\nExample:");
        description.append("\n  Q: "); description.append(QuestionExample);
        description.append("\n  A: "); description.append(AnswerExample);
        spec.function.description = description.toString();

        var parameters = new McpToolCallParameters();
        parameters.type = "object";

        var properties = new HashMap<String, McpToolCallProperty>();

        var actionProp = new McpToolCallProperty();
        actionProp.type = "string";
        actionProp.description = "Operation to perform. \"save\" (default) - validate `content`, write it to `path` in UTF-8 and show the rendered image in the chat. \"check\" - validate and sanitize `content` only; nothing is written; the diagnostics come back with line and column numbers. \"preview\" - read the existing SVG file at `path` and show it in the chat.";
        properties.put("action", actionProp);

        var pathProp = new McpToolCallProperty();
        pathProp.type = "string";
        pathProp.description = "Absolute path to the `.svg` file. Required for \"save\" and \"preview\". For \"check\" nothing is written - pass the path you intend to use. The project is auto-detected from the absolute path.";
        properties.put("path", pathProp);

        var contentProp = new McpToolCallProperty();
        contentProp.type = "string";
        contentProp.description = "The complete SVG markup, starting with the `<svg>` root element. Required for \"save\" and \"check\"; ignored for \"preview\".";
        properties.put("content", contentProp);

        var overwriteProp = new McpToolCallProperty();
        overwriteProp.type = "boolean";
        overwriteProp.description = "Set to true to replace an existing non-empty file at `path`. Default is false, so that an existing drawing is never lost by accident.";
        properties.put("overwrite", overwriteProp);

        var titleProp = new McpToolCallProperty();
        titleProp.type = "string";
        titleProp.description = "Optional short caption shown above the image in the chat, for example \"Document posting flow\".";
        properties.put("title", titleProp);

        parameters.properties = properties;
        parameters.required = Arrays.asList("path");

        spec.function.parameters = parameters;
        return spec;
     // @formatter:on
    }

    private static class Request
    {
        /**
         * Operation: "save" (default), "check" or "preview".
         */
        @SerializedName("action")
        public String action;

        /**
         * Absolute path to the .svg file. Required.
         */
        @SerializedName("path")
        public String path;

        /**
         * SVG markup. Required for "save" and "check".
         */
        @SerializedName("content")
        public String content;

        /**
         * Allow replacing an existing non-empty file. Default false.
         */
        @SerializedName("overwrite")
        public Boolean overwrite;

        /**
         * Optional caption shown above the preview.
         */
        @SerializedName("title")
        public String title;
    }
}
