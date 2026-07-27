/*
 * Copyright (C) 2026, 1C
 */
package com.e1c.edt.ai;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;

import com.e1c.edt.ai.assistent.ConversationSession;
import com.e1c.edt.ai.assistent.SendMessageResult;
import com.e1c.edt.ai.assistent.SendUserMessageRequest;
import com.google.common.base.Preconditions;
import com.google.gson.annotations.SerializedName;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * File-driven dev harness. See {@link IDevAutopilot} and the dev-autopilot README.
 * <p>
 * Channel directory resolution: system property {@code ai.dev.channel.dir}, otherwise
 * {@code <workspace>/.metadata/ai-dev} (the Eclipse instance/workspace location), with a
 * {@code java.io.tmpdir} fallback. The resolved absolute path is logged on start so an external
 * agent can discover it.
 */
@Singleton
public class DevAutopilot
    implements IDevAutopilot
{
    /** System property overriding the channel directory. */
    public static final String CHANNEL_DIR_PROPERTY = "ai.dev.channel.dir"; //$NON-NLS-1$

    private static final long POLL_INTERVAL_MS = 1000L;
    // Limits are intentionally very high so a long turnkey build (a whole configuration with entities,
    // forms, templates, and code modules) is never truncated mid-run. They remain finite only as a
    // runaway backstop.
    private static final long TURN_TIMEOUT_SECONDS = 3600L;
    private static final int MAX_AUTO_CONTINUES = 100;

    /**
     * Default agent preamble prepended to the user prompt. The dev/helper conversation (skill
     * {@code custom}) lacks the interactive chat's agent system prompt, so without this the model
     * tends to gather context and stop. A request may override it via {@code preamble} (use an
     * empty string to send the bare prompt).
     */
    private static final String DEFAULT_PREAMBLE =
        "Не отвечай одним планом и не останавливайся. Инструмента TodoWrite нет: не вызывай его. " //$NON-NLS-1$
            + "Никогда не создавай и не исправляй метаданные прямым редактированием .mdo/.form/XML. " //$NON-NLS-1$
            + "Используй 1C_EditMetadata; ошибочный дочерний элемент удали и создай заново его операциями.\n\nЗадача: "; //$NON-NLS-1$

    private final IConversationFacade conversationFacade;
    private final IDevToolCallRecorder recorder;
    private final IMcpTools mcpTools;
    private final IJson json;
    private final ILog log;

    private volatile boolean running;
    private Thread worker;

    @Inject
    public DevAutopilot(IConversationFacade conversationFacade, IDevToolCallRecorder recorder, IMcpTools mcpTools,
        IJson json, ILog log)
    {
        Preconditions.checkNotNull(conversationFacade);
        Preconditions.checkNotNull(recorder);
        Preconditions.checkNotNull(mcpTools);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(log);
        this.conversationFacade = conversationFacade;
        this.recorder = recorder;
        this.mcpTools = mcpTools;
        this.json = json;
        this.log = log;
    }

    @Override
    public synchronized void start()
    {
        if (running)
        {
            return;
        }

        Path channel = resolveChannelDir();
        Path inbox = channel.resolve("inbox"); //$NON-NLS-1$
        Path processing = channel.resolve("processing"); //$NON-NLS-1$
        Path outbox = channel.resolve("outbox"); //$NON-NLS-1$
        try
        {
            Files.createDirectories(inbox);
            Files.createDirectories(processing);
            Files.createDirectories(outbox);
        }
        catch (IOException e)
        {
            log.logError(e);
            return;
        }

        running = true;
        worker = new Thread(() -> pollLoop(inbox, processing, outbox), "ai-dev-autopilot"); //$NON-NLS-1$
        worker.setDaemon(true);
        worker.start();
        log.logError("[dev-autopilot] started. Channel dir: " + channel.toAbsolutePath() //$NON-NLS-1$
            + " (drop request *.json into 'inbox', read transcripts from 'outbox')"); //$NON-NLS-1$
    }

    @Override
    public synchronized void stop()
    {
        running = false;
        if (worker != null)
        {
            worker.interrupt();
            worker = null;
        }
    }

    private Path resolveChannelDir()
    {
        String configured = System.getProperty(CHANNEL_DIR_PROPERTY);
        if (configured != null && !configured.isBlank())
        {
            return Paths.get(configured);
        }
        // Default: <workspace>/.metadata/ai-dev (e.g. D:\Projects\_Eclipse\EDT_Plugin\.metadata\ai-dev).
        var workspaceLocation = ResourcesPlugin.getWorkspace().getRoot().getLocation();
        if (workspaceLocation != null)
        {
            return Paths.get(workspaceLocation.toOSString(), ".metadata", "ai-dev"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        // Fallback when the workspace location is unavailable.
        return Paths.get(System.getProperty("java.io.tmpdir"), "edt-ai-dev"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private void pollLoop(Path inbox, Path processing, Path outbox)
    {
        while (running)
        {
            try
            {
                processInbox(inbox, processing, outbox);
                Thread.sleep(POLL_INTERVAL_MS);
            }
            catch (InterruptedException e)
            {
                Thread.currentThread().interrupt();
                return;
            }
            catch (Exception e)
            {
                log.logError(e);
            }
        }
    }

    private void processInbox(Path inbox, Path processing, Path outbox) throws InterruptedException
    {
        List<Path> requests = new ArrayList<>();
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(inbox, "*.json")) //$NON-NLS-1$
        {
            for (Path p : stream)
            {
                requests.add(p);
            }
        }
        catch (IOException e)
        {
            log.logError(e);
            return;
        }

        requests.sort((a, b) -> a.getFileName().toString().compareTo(b.getFileName().toString()));
        for (Path req : requests)
        {
            if (!running)
            {
                return;
            }
            processOne(req, processing, outbox);
        }
    }

    private void processOne(Path req, Path processing, Path outbox)
    {
        String id = stripExtension(req.getFileName().toString());
        Path moved = processing.resolve(req.getFileName());
        Response response = new Response();
        response.id = id;
        long start = System.nanoTime();
        try
        {
            try
            {
                Files.move(req, moved, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException e)
            {
                // another iteration or external mover took it; skip
                return;
            }

            String raw = Files.readString(moved, StandardCharsets.UTF_8);
            Request request = json.deserialize(raw, Request.class).orElse(null);
            if (request == null || request.prompt == null || request.prompt.isBlank())
            {
                response.error = "Invalid request: 'prompt' is required"; //$NON-NLS-1$
            }
            else
            {
                response.prompt = request.prompt;
                response.project = request.project;
                runTurn(request, response);
            }
        }
        catch (Exception e)
        {
            response.error = e.toString();
            log.logError(e);
        }
        finally
        {
            response.durationMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
            writeOutbox(outbox, id, response);
            try
            {
                Files.deleteIfExists(moved);
            }
            catch (IOException ignore)
            {
                // best effort
            }
        }
    }

    private void runTurn(Request request, Response response) throws Exception
    {
        IProject project = resolveProject(request.project)
            .orElseThrow(() -> new IllegalArgumentException("Project is required and must exist: " + request.project)); //$NON-NLS-1$
        var promptText = applyPreamble(request);
        // Multi-artifact metadata tasks (object + form/template + content) plus self-correction
        // need more than the default 10 tool rounds; default the harness to a higher cap.
        // Unbounded tool nesting by default (see Conversations.MAX_TOOL_ROUNDS): a turnkey build must
        // not be cut off mid-run. A request may still pass an explicit lower cap.
        Integer maxToolRounds =
            request.maxToolRounds != null ? request.maxToolRounds : Integer.valueOf(Integer.MAX_VALUE);
        // The dev-autopilot must run on the "custom" skill regardless of the ConversationFacade
        // default (which is "edt"). Honor an explicit per-request override (e.g. "edt" for routing
        // diagnostics); otherwise force "custom".
        String skill = request.skill != null ? request.skill : "custom"; //$NON-NLS-1$
        Boolean isChat = request.isChat != null ? request.isChat : Boolean.TRUE;
        // Fixed-prelude size (helps assess the "large context" hypothesis): all tool definitions
        // sent on every turn, independent of chat history.
        captureToolsMetrics(response);

        recorder.beginRun();
        try
        {
            ConversationSession session = null;
            String nextPrompt = promptText;
            boolean forceNewConversation = true;
            for (int autoContinue = 0; autoContinue <= MAX_AUTO_CONTINUES; autoContinue++)
            {
                var message = new SendUserMessageRequest(project, nextPrompt, session, forceNewConversation, skill,
                    isChat, maxToolRounds);
                SendMessageResult result = conversationFacade.sendAsync(message, CancellationTokens.NONE)
                    .get(TURN_TIMEOUT_SECONDS, TimeUnit.SECONDS);
                if (result != null)
                {
                    applyResult(response, result);
                }
                if (!shouldAutoContinue(result, autoContinue))
                {
                    break;
                }
                session = result.getSession();
                if (session == null)
                {
                    break;
                }
                response.autoContinueCount = autoContinue + 1;
                nextPrompt = autoContinuePrompt(request);
                forceNewConversation = false;
            }
        }
        finally
        {
            response.toolCalls = recorder.endRun();
            response.toolCallCount = response.toolCalls != null ? response.toolCalls.size() : 0;
            response.stalled = response.toolCalls == null
                || response.toolCalls.stream().noneMatch(c -> isMutationTool(c.tool));
            captureToolFailureMetrics(response);
        }
    }

    private void captureToolFailureMetrics(Response response)
    {
        if (response.toolCalls == null)
        {
            return;
        }
        response.toolErrorCount = (int)response.toolCalls.stream()
            .filter(c -> c.error != null && !c.error.isBlank())
            .count();
        response.jshellErrorCount = (int)response.toolCalls.stream()
            .filter(c -> "jshell".equalsIgnoreCase(c.tool)) //$NON-NLS-1$
            .filter(c -> hasNonEmptyJsonArray(c.result, "compilation_errors") //$NON-NLS-1$
                || hasNonEmptyJsonArray(c.result, "runtime_errors")) //$NON-NLS-1$
            .count();
        response.hasToolFailures = response.toolErrorCount > 0 || response.jshellErrorCount > 0;
    }

    public static boolean isMutationTool(String tool)
    {
        return "jshell".equalsIgnoreCase(tool) || "1c_editmetadata".equalsIgnoreCase(tool); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private boolean hasNonEmptyJsonArray(String text, String field)
    {
        if (text == null || text.isBlank())
        {
            return false;
        }
        String marker = "\"" + field + "\""; //$NON-NLS-1$ //$NON-NLS-2$
        int index = text.indexOf(marker);
        if (index < 0)
        {
            return false;
        }
        int colon = text.indexOf(':', index + marker.length());
        if (colon < 0)
        {
            return false;
        }
        int bracket = text.indexOf('[', colon + 1);
        if (bracket < 0)
        {
            return false;
        }
        int valueStart = bracket + 1;
        while (valueStart < text.length() && Character.isWhitespace(text.charAt(valueStart)))
        {
            valueStart++;
        }
        return valueStart < text.length() && text.charAt(valueStart) != ']';
    }

    private void applyResult(Response response, SendMessageResult result)
    {
        response.finalText = result.getText();
        response.reasoning = result.getReasoning();
        response.assistantMessageCount += result.getAssistantMessageCount();
        if (result.getSession() != null)
        {
            response.conversationId = result.getSession().getConversationId();
            response.replyToMessageUuid = result.getSession().getReplyToMessageUuid();
        }
    }

    private boolean shouldAutoContinue(SendMessageResult result, int autoContinue)
    {
        if (result == null || autoContinue >= MAX_AUTO_CONTINUES || result.getSession() == null)
        {
            return false;
        }
        String text = result.getText() != null ? result.getText().trim().toLowerCase() : ""; //$NON-NLS-1$
        String reasoning = result.getReasoning() != null ? result.getReasoning().toLowerCase() : ""; //$NON-NLS-1$
        if (text.isBlank())
        {
            return true;
        }
        return text.startsWith("создам") || text.startsWith("начну") //$NON-NLS-1$ //$NON-NLS-2$
            || reasoning.contains("начну с создания") || reasoning.contains("теперь начну") //$NON-NLS-1$ //$NON-NLS-2$
            || reasoning.contains("следующий шаг") || reasoning.contains("нужно вызвать jshell"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    private String autoContinuePrompt(Request request)
    {
        return targetProjectInstruction(request)
            + "Продолжай немедленно инструментами в этой же задаче. Не отвечай планом и не пиши \"создам\". " //$NON-NLS-1$
            + "Метаданные 1С (объекты, реквизиты, формы, макеты, конфигурация) создавай/меняй/удаляй ТОЛЬКО через 1C_EditMetadata; " //$NON-NLS-1$
            + "не редактируй метаданные через JShell и не правь .mdo/.form напрямую. " //$NON-NLS-1$
            + "Следующее действие должно быть tool call: 1C_EditMetadata или GetMarkers. Затем проверяй маркеры."; //$NON-NLS-1$
    }

    private void captureToolsMetrics(Response response)
    {
        try
        {
            var specs = mcpTools.getSpecifications().get(30, TimeUnit.SECONDS);
            response.toolsCount = specs != null ? specs.size() : 0;
            response.toolsDefinitionChars = specs != null ? json.serialize(specs).length() : 0;
        }
        catch (Exception e)
        {
            // diagnostics only — never fail the turn over this
            response.toolsCount = -1;
            response.toolsDefinitionChars = -1;
        }
    }

    private String applyPreamble(Request request)
    {
        // null preamble => default agent preamble; blank preamble => bare prompt; otherwise custom.
        if (request.preamble == null)
        {
            return DEFAULT_PREAMBLE + targetProjectInstruction(request) + request.prompt;
        }
        if (request.preamble.isBlank())
        {
            return request.prompt;
        }
        return request.preamble + "\n\n" + targetProjectInstruction(request) + request.prompt; //$NON-NLS-1$
    }

    private String targetProjectInstruction(Request request)
    {
        if (request.project == null || request.project.isBlank())
        {
            return ""; //$NON-NLS-1$
        }
        return "Целевой проект запроса: \"" + request.project //$NON-NLS-1$
            + "\". Это точный идентификатор проекта, а не доменное слово; похожие имена других проектов запрещены. " //$NON-NLS-1$
            + "Все чтение, JShell-код, GetMarkers и изменения должны относиться к этому проекту. " //$NON-NLS-1$
            + "Не используй NavigationHistory/current editor как цель, если он указывает на другой проект; " //$NON-NLS-1$
            + "для продолжений вроде \"Добавь все необходимое\" работай с целевым проектом через " //$NON-NLS-1$
            + "scaffold_business_configuration и минимальный проверяемый набор объектов. " //$NON-NLS-1$
            + "Нельзя изменять файлы других проектов.\n\n"; //$NON-NLS-1$
    }

    private Optional<IProject> resolveProject(String projectName)
    {
        if (projectName == null || projectName.isBlank())
        {
            return Optional.empty();
        }
        var project = ResourcesPlugin.getWorkspace().getRoot().getProject(projectName);
        if (project != null && project.exists())
        {
            return Optional.of(project);
        }
        return Optional.empty();
    }

    private void writeOutbox(Path outbox, String id, Response response)
    {
        try
        {
            Path target = outbox.resolve(id + ".json"); //$NON-NLS-1$
            Files.writeString(target, json.serialize(response), StandardCharsets.UTF_8);
        }
        catch (IOException e)
        {
            log.logError(e);
        }
    }

    private static String stripExtension(String fileName)
    {
        int dot = fileName.lastIndexOf('.');
        return dot > 0 ? fileName.substring(0, dot) : fileName;
    }

    private static class Request
    {
        @SerializedName("prompt")
        public String prompt;

        @SerializedName("project")
        public String project;

        /** Optional skill override (e.g. "custom", "raw", "system"); null keeps the default. */
        @SerializedName("skill")
        public String skill;

        /** Optional is_chat override; null keeps the default. */
        @SerializedName("is_chat")
        public Boolean isChat;

        /**
         * Optional agent preamble prepended to the prompt: {@code null} → default preamble,
         * {@code ""}/blank → bare prompt (no preamble), any other string → that preamble.
         */
        @SerializedName("preamble")
        public String preamble;

        /** Optional tool-round cap; null → harness default (200). */
        @SerializedName("max_tool_rounds")
        public Integer maxToolRounds;
    }

    private static class Response
    {
        @SerializedName("id")
        public String id;

        @SerializedName("prompt")
        public String prompt;

        @SerializedName("project")
        public String project;

        @SerializedName("conversation_id")
        public String conversationId;

        @SerializedName("reply_to_message_uuid")
        public String replyToMessageUuid;

        @SerializedName("final_text")
        public String finalText;

        /** Final assistant message's reasoning_content (chain-of-thought) — explains stalls/choices. */
        @SerializedName("reasoning")
        public String reasoning;

        /** Number of finished assistant messages (model turns); a stall is typically 1 empty message. */
        @SerializedName("assistant_message_count")
        public int assistantMessageCount;

        /** Number of automatic "continue with tools" nudges sent inside this harness request. */
        @SerializedName("auto_continue_count")
        public int autoContinueCount;

        @SerializedName("tool_calls")
        public List<IDevToolCallRecorder.DevToolCall> toolCalls;

        @SerializedName("tool_call_count")
        public int toolCallCount;

        /** Tool calls that returned a top-level tool error. */
        @SerializedName("tool_error_count")
        public int toolErrorCount;

        /** JShell calls whose result contains compilation_errors or runtime_errors. */
        @SerializedName("jshell_error_count")
        public int jshellErrorCount;

        /** True when any tool or JShell execution failed at least once during the request. */
        @SerializedName("has_tool_failures")
        public boolean hasToolFailures;

        /** True when the turn ran neither `jshell` nor `1c_editmetadata`. */
        @SerializedName("stalled")
        public boolean stalled;

        /** Number of MCP tool definitions sent every turn (fixed prelude). */
        @SerializedName("tools_count")
        public int toolsCount;

        /** Serialized size (chars) of all tool definitions — proxy for fixed-prelude weight. */
        @SerializedName("tools_definition_chars")
        public int toolsDefinitionChars;

        @SerializedName("error")
        public String error;

        @SerializedName("duration_ms")
        public long durationMs;
    }
}
