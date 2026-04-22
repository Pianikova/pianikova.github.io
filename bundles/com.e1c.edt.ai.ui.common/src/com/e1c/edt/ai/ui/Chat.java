/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.egit.ui.internal.commit.DiffDocument;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.ui.PlatformUI;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IContextEntities;
import com.e1c.edt.ai.IFileDocument;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILocalContext;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IMcpTools;
import com.e1c.edt.ai.IProjectTools;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.McpCallToolsResult;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.ISessionService;
import com.e1c.edt.ai.assistent.IStateListener;
import com.e1c.edt.ai.assistent.model.ChatContext;
import com.e1c.edt.ai.assistent.model.LocalContext;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

import javafx.beans.value.ChangeListener;
import javafx.concurrent.Worker.State;
import javafx.scene.control.ScrollPane;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * @author George Suaridze
 *
 */
@SuppressWarnings("restriction")
public class Chat
    implements IChat, IChatDialog, IStateListener
{
    private static final String AI_CHAT_DIR = "ai.chat"; //$NON-NLS-1$
    private static final String INSTANCE_ID = UUID.randomUUID().toString();
    private static final java.util.concurrent.atomic.AtomicBoolean CLEANUP_REGISTERED =
        new java.util.concurrent.atomic.AtomicBoolean();

    private static final String AI_CHAT = "AI Chat"; //$NON-NLS-1$
    private static final String CHAT_API_WINK_TEMPLATE =
        "window.chatApi.wink({client_id: \"%s\", client_uid: \"%s\"}, \"%s\", \"%s\")"; //$NON-NLS-1$
    private static final String IDE_API = "ideApi"; //$NON-NLS-1$
    private static final String EMPTY_STRING = "``"; //$NON-NLS-1$
    private static final String NULL_VALUE = "null"; //$NON-NLS-1$
    private static final Character ARGS_SEPARATOR = ',';
    private static final String WINDOW_CHAT_API_SET_TOOLS = "window.chatApi.set_tools("; //$NON-NLS-1$
    private static final String WINDOW = "window"; //$NON-NLS-1$
    private static final String TOPIC_INSERT_CODE = "insert_code"; //$NON-NLS-1$

    private final ILog log;
    private final ISettings settings;
    private final IUI ui;
    private final IDispatcher dispatcher;
    private final IdeApiHandler handler;
    private final IContextEntities contextEntities;
    private final IJavaScript javaScript;
    private final IStateService stateService;
    private final ISessionService sessionService;
    private final IModuleNameProvider moduleNameProvider;
    private final ILocalContext localContext;
    private final IProposalsProvider proposalsProvider;
    private final IJson json;
    private final IMcpTools mcpTools;
    private final IEdtLinkHandler linkHandler;
    private final IProjectTools projectTools;
    private final IContentSourceProvider contentSourceProvider;
    private final IFileSystem fileSystem;
    private final Cache<String, AIContext> contexts = CacheBuilder.newBuilder().maximumSize(256).weakKeys().build();
    private final List<ChangeListener<State>> initializationListeners = new ArrayList<>();

    private WebView webView;
    private ChatKey lastChatKey;
    private String lastDialogPath;
    private ChangeListener<Number> widthListener;
    private ChangeListener<Number> heightListener;
    private boolean isFirst = true;

    @Inject
    public Chat(ILog log, ISettings settings, IUI ui, IDispatcher dispatcher, IdeApiHandler handler,
        IContextEntities contextEntities, IJavaScript javaScript, IStateService stateService,
        ISessionService sessionService, IModuleNameProvider moduleNameProvider, ILocalContext localContext,
        IProposalsProvider proposalsProvider, IJson json, IMcpTools mcpTools, IEdtLinkHandler linkHandler,
        IProjectTools projectTools, IContentSourceProvider contentSourceProvider, IFileSystem fileSystem)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(handler);
        Preconditions.checkNotNull(contextEntities);
        Preconditions.checkNotNull(javaScript);
        Preconditions.checkNotNull(stateService);
        Preconditions.checkNotNull(sessionService);
        Preconditions.checkNotNull(moduleNameProvider);
        Preconditions.checkNotNull(localContext);
        Preconditions.checkNotNull(proposalsProvider);
        Preconditions.checkNotNull(json);
        Preconditions.checkNotNull(mcpTools);
        Preconditions.checkNotNull(linkHandler);
        Preconditions.checkNotNull(projectTools);
        Preconditions.checkNotNull(contentSourceProvider);
        Preconditions.checkNotNull(fileSystem);
        this.log = log;
        this.settings = settings;
        this.ui = ui;
        this.dispatcher = dispatcher;
        this.handler = handler;
        this.contextEntities = contextEntities;
        this.javaScript = javaScript;
        this.stateService = stateService;
        this.sessionService = sessionService;
        this.moduleNameProvider = moduleNameProvider;
        this.localContext = localContext;
        this.proposalsProvider = proposalsProvider;
        this.json = json;
        this.mcpTools = mcpTools;
        this.linkHandler = linkHandler;
        this.projectTools = projectTools;
        this.contentSourceProvider = contentSourceProvider;
        this.fileSystem = fileSystem;

        stateService.addListener(this);
    }

    @Override
    public void reviewCode(AIContext ctx, String codeSnippet)
    {
        Preconditions.checkNotNull(codeSnippet);
        chat("review_code", codeSnippet, null, ctx); //$NON-NLS-1$
    }

    @Override
    public void explainCode(AIContext ctx, String codeSnippet)
    {
        Preconditions.checkNotNull(codeSnippet);
        chat("comment_code", codeSnippet, null, ctx); //$NON-NLS-1$
    }

    @Override
    public void fixCode(AIContext ctx, String codeSnippet, String details)
    {
        Preconditions.checkNotNull(codeSnippet);
        chat("fix_code", codeSnippet, details, ctx); //$NON-NLS-1$
    }

    @Override
    public void generateDocComments(AIContext ctx, String method)
    {
        Preconditions.checkNotNull(method);
        chat("document_code", method, null, ctx); //$NON-NLS-1$
    }

    @Override
    public void askQuestion(AIContext ctx, String userQuestion)
    {
        Preconditions.checkNotNull(userQuestion);
        chat("plain_message", userQuestion, null, ctx); //$NON-NLS-1$
    }

    @Override
    public void addCode(AIContext ctx, String codeSnippet)
    {
        Preconditions.checkNotNull(codeSnippet);
        chat(TOPIC_INSERT_CODE, codeSnippet, null, ctx);
    }

    @SuppressWarnings("nls")
    @Override
    public void addToolsResult(String chatId, String messageId, McpCallToolsResult result)
    {
        var ctx = getContext(chatId);
        chatInJob(ctx, () -> {
            try (var busyToken = stateService.busy())
            {
                var messagesJson = result.messages != null ? json.serialize(result.messages) : null;

                var unknownCallsJson = result.unknownCalls != null ? json.serialize(result.unknownCalls) : null;

                dispatcher.dispatchAsync(() -> {
                    var webEngine = getEngine();
                    var window = (JSObject)webEngine.executeScript(WINDOW);
                    if (window != null)
                    {
                        window.setMember("calls_messages", messagesJson);
                        window.setMember("unknown_messages", unknownCallsJson);
                    }

                    var script = String.format(
                        "window.chatApi.add_tool_calls_result(%s, %s, window.calls_messages, window.unknown_messages);",
                        javaScript.escape(chatId, EMPTY_STRING), javaScript.escape(messageId, EMPTY_STRING));
                    executeScriptWithLogging(script);
                });
            }
            catch (Exception error)
            {
                log.logError(error);
            }
        });
    }

    @SuppressWarnings("nls")
    @Override
    public void continueChat(String chatId, String text)
    {
        var ctx = getContext(chatId);
        chatInJob(ctx, () -> {
            try (var busyToken = stateService.busy())
            {
                var script = String.format("window.chatApi.continue_chat(%s, %s);",
                    javaScript.escape(text, EMPTY_STRING), javaScript.escape(chatId, NULL_VALUE));
                dispatcher.dispatchAsync(() -> executeScriptWithLogging(script));
            }
            catch (Exception error)
            {
                log.logError(error);
            }
        });
    }

    @Override
    public void addFiles(List<IFileDocument> documents)
    {
        var errorReadingFile = new StringBuilder();
        if (documents == null)
        {
            documents = openFilesAndCreateDocuments(errorReadingFile);
            if (documents == null || documents.isEmpty())
            {
                showErrorIfAny(errorReadingFile);
                return;
            }
        }

        for (var document : documents)
        {
            var ctx = new AIContext(document.getProjectId(), document.getFile().getLocation().toPortableString(), null);
            chat(TOPIC_INSERT_CODE, document.getDocument().get(), null, ctx);
        }

        showErrorIfAny(errorReadingFile);
    }

    private List<IFileDocument> openFilesAndCreateDocuments(StringBuilder errorReadingFile)
    {
        var shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        var dialog = new FileDialog(shell, SWT.OPEN | SWT.MULTI);
        dialog.setText(Messages.AddFilesToChatDialogName);
        dialog.setFilterPath(lastDialogPath);
        var file = dialog.open();
        if (file == null)
        {
            return null;
        }

        lastDialogPath = dialog.getFilterPath();
        var documents = new ArrayList<IFileDocument>();
        for (var fileName : dialog.getFileNames())
        {
            var filePath = lastDialogPath != null ? Path.of(lastDialogPath, fileName) : Path.of(fileName.toString());
            readFileContent(filePath, fileName, errorReadingFile);
        }
        return documents;
    }

    private void readFileContent(Path filePath, String fileName, StringBuilder errorReadingFile)
    {
        var pathString = filePath.toString();

        try
        {
            String content;
            AIContext ctx;

            var projectName = projectTools.determineProjectName(pathString);
            var isProjectFile = projectName != null && !projectName.isBlank();

            if (isProjectFile)
            {
                var root = ResourcesPlugin.getWorkspace().getRoot();
                var project = root.getProject(projectName);

                if (project != null && project.exists())
                {
                    var optionalFile = projectTools.getProjectFile(project, pathString);
                    if (optionalFile.isPresent())
                    {
                        var optionalDocument = contentSourceProvider.getFileDocument(optionalFile.get());
                        if (optionalDocument.isPresent())
                        {
                            var document = optionalDocument.get();
                            content = document.getDocument().get();
                            ctx = new AIContext(new ProjectId(project), pathString, document.getDocument());
                        }
                        else
                        {
                            throw new IOException("Cannot get document for project file"); //$NON-NLS-1$
                        }
                    }
                    else
                    {
                        throw new IOException("Cannot get IFile for project"); //$NON-NLS-1$
                    }
                }
                else
                {
                    throw new IOException("Project does not exist or is not accessible"); //$NON-NLS-1$
                }
            }
            else
            {
                var bytes = Files.readAllBytes(filePath);
                content = new String(bytes, StandardCharsets.UTF_8);
                ctx = new AIContext(ProjectId.Default, pathString, null);
            }

            if (!fileSystem.isPrintable(content, 90.0))
            {
                errorReadingFile.append(MessageFormat.format(Messages.FileNotTextFormat, fileName));
                errorReadingFile.append(System.lineSeparator());
                return;
            }

            chat(TOPIC_INSERT_CODE, content, null, ctx);
        }
        catch (IOException e)
        {
            errorReadingFile.append(MessageFormat.format(Messages.ErrorReadingFile, fileName));
            errorReadingFile.append(System.lineSeparator());
        }
    }

    private void showErrorIfAny(StringBuilder errorReadingFile)
    {
        if (errorReadingFile.length() > 0)
        {
            var shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
            MessageDialog.openError(shell, Messages.ErrorReadingTextFile, errorReadingFile.toString());
        }
    }

    @SuppressWarnings("nls")
    private void chat(String topic, String subject, String details, AIContext ctx)
    {
        ui.showView(BaseChatView.ID);
        chatInJob(Optional.ofNullable(ctx), () -> {
            try (var busyToken = stateService.busy())
            {
                var sessionId = getSessionId(ctx);
                if (sessionId.isEmpty())
                {
                    log.warning(AI_CHAT, () -> "Cannot get session id");
                    return;
                }

                var contextInfo = buildContextInfo(ctx);
                var script = buildChatScript(topic, subject, details, ctx, sessionId.get(), contextInfo);

                dispatcher.dispatchAsync(() -> {
                    var executeScriptResult = executeScriptWithLogging(script);
                    if (executeScriptResult instanceof String)
                    {
                        var chatId = (String)executeScriptResult;
                        contexts.put(chatId, ctx);
                    }
                });
            }
            catch (Exception error)
            {
                log.logError(error);
            }
        });
    }

    private Optional<String> getSessionId(AIContext ctx)
    {
        var projectId = Optional.ofNullable(ctx).map(AIContext::getProjectId).orElse(ProjectId.Default);
        try
        {
            return sessionService.getSessionAsync(projectId).get().map(i -> i.sessionId);
        }
        catch (InterruptedException | ExecutionException e)
        {
            return Optional.empty();
        }
    }

    private ContextInfo buildContextInfo(AIContext ctx)
    {
        var info = new ContextInfo();
        if (ctx == null)
        {
            info.title = NULL_VALUE;
            return info;
        }

        info.title = moduleNameProvider.getModuleName(ctx.getPath()).orElseGet(() -> ctx.getPath());
        var chatContext = new ChatContext();
        var doc = ctx.getDocument();
        contextEntities.fill(ctx, chatContext, IStatistics.Empty, CancellationTokens.NONE);
        info.scriptLanguage = chatContext.scriptLanguage;
        info.programingLanguage = chatContext.programingLanguage;

        if (doc instanceof DiffDocument)
        {
            info.programingLanguage = "git diff"; //$NON-NLS-1$
        }

        return info;
    }

    @SuppressWarnings("nls")
    private String buildChatScript(String topic, String subject, String details, AIContext ctx, String sessionId,
        ContextInfo contextInfo)
    {
        var script = new StringBuilder();
        script.append("window.chatApi.");
        script.append(topic);
        script.append('(');
        script.append(javaScript.escape(subject, EMPTY_STRING));
        script.append(ARGS_SEPARATOR);
        script.append(javaScript.escape(contextInfo.scriptLanguage, EMPTY_STRING));
        script.append(ARGS_SEPARATOR);
        script.append(javaScript.escape(contextInfo.programingLanguage, EMPTY_STRING));

        if (details != null)
        {
            script.append(ARGS_SEPARATOR);
            script.append(javaScript.escape(details, EMPTY_STRING));
        }

        script.append(ARGS_SEPARATOR);
        var path = Optional.ofNullable(ctx).map(AIContext::getPath).orElse(null);
        if (TOPIC_INSERT_CODE.equals(topic))
        {
            path = linkHandler.getFullPathForInsertCode(ctx);
            path = linkHandler.formatInsertCodePath(ctx, path);
        }

        script.append(javaScript.escape(path, NULL_VALUE));

        if (topic.equals(TOPIC_INSERT_CODE))
        {
            appendLineNumbers(script, ctx);
        }

        script.append(ARGS_SEPARATOR);
        script.append(javaScript.escape(sessionId, NULL_VALUE));
        script.append(ARGS_SEPARATOR);
        script.append(javaScript.escape(contextInfo.title, NULL_VALUE));

        var contextJson = json.serialize(buildContext(ctx));
        script.append(ARGS_SEPARATOR);
        script.append(javaScript.escape(contextJson, NULL_VALUE));
        script.append(");");

        return script.toString();
    }

    private LocalContext buildContext(AIContext ctx)
    {
        var context = localContext.create(ctx, IStatistics.Empty, CancellationTokens.NONE);
        var sourceViewer = ui.getLastSourceViewer();
        if (sourceViewer.isPresent())
        {
            context.proposals =
                proposalsProvider.getProposals(ctx, sourceViewer.get(), 600, CancellationTokens.NONE).orElse(null);
        }
        return context;
    }

    private void appendLineNumbers(StringBuilder script, AIContext ctx)
    {
        var document = Optional.ofNullable(ctx).map(AIContext::getDocument).orElse(null);
        Integer startLine = null, endLine = null;
        if (document != null)
        {
            try
            {
                startLine = document.getLineOfOffset(ctx.getStart());
                endLine = document.getLineOfOffset(ctx.getFinish());
            }
            catch (BadLocationException error)
            {
                log.logError(error);
            }
        }

        script.append(ARGS_SEPARATOR);
        script.append(startLine != null ? startLine.toString() : NULL_VALUE);
        script.append(ARGS_SEPARATOR);
        script.append(endLine != null ? endLine.toString() : NULL_VALUE);
    }

    @SuppressWarnings("nls")
    private Object executeScriptWithLogging(String script)
    {
        log.trace(TracingSources.CHAT, AI_CHAT, () -> "executing script: " + script);
        var executeScriptResult = getEngine().executeScript(script);
        log.trace(TracingSources.CHAT, AI_CHAT, () -> "script executed: " + executeScriptResult);
        return executeScriptResult;
    }

    @Override
    public void show(ScrollPane pane)
    {
        ensureWebViewExists();
        pane.setContent(webView);
        webView.setFocusTraversable(true);
        webView.setPrefWidth(pane.getWidth());
        webView.setPrefHeight(pane.getHeight());

        widthListener = (observable, oldValue, newValue) -> webView.setPrefWidth((Double)newValue);
        heightListener = (observable, oldValue, newValue) -> webView.setPrefHeight((Double)newValue);

        pane.widthProperty().addListener(widthListener);
        pane.heightProperty().addListener(heightListener);

        warmUp();
    }

    private void ensureWebViewExists()
    {
        if (webView != null)
        {
            return;
        }

        var view = new WebView();
        webView = view;
        view.setLayoutX(-1);
        view.setLayoutY(-1);

        var webEngine = getEngine();
        var userDataDirectory = getUserDataDirectory();
        try
        {
            Files.createDirectories(userDataDirectory);
        }
        catch (IOException error)
        {
            log.logError(error);
        }
        webEngine.setUserDataDirectory(userDataDirectory.toFile());
        registerCleanupHook(userDataDirectory);
        webEngine.setOnError(event -> {
            log.logError(event.getMessage());
            log.logError(event.getException());
        });

        view.setOnDragOver(event -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles())
            {
                event.acceptTransferModes(TransferMode.COPY);
            }

            event.consume();
        });

        view.setOnDragDropped(event -> {
            Dragboard dragboard = event.getDragboard();
            if (dragboard.hasFiles())
            {
                var errorReadingFile = new StringBuilder();
                for (var file : dragboard.getFiles())
                {
                    readFileContent(file.toPath(), file.getName(), errorReadingFile);
                }
                showErrorIfAny(errorReadingFile);
                event.setDropCompleted(true);
            }
            else
            {
                event.setDropCompleted(false);
            }

            event.consume();
        });

        webEngine.loadContent(createLoadingPage());

        var worker = webEngine.getLoadWorker();
        worker.runningProperty()
            .addListener((observable, oldValue, newValue) -> log.trace(TracingSources.CHAT, AI_CHAT,
                () -> "is running: " + newValue)); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private String createLoadingPage()
    {
        var theme = settings.getTheme();
        var isDark = "dark".equalsIgnoreCase(theme);

        var title = Messages.ChatLoadingTitle;
        var message = Messages.ChatLoadingMessage;

        var backgroundColor = isDark ? "#1e1e1e" : "#ffffff";
        var textColor = isDark ? "#cccccc" : "#333333";
        var spinnerColor = isDark ? "#4fc3f7" : "#0288d1";
        var spinnerBg = isDark ? "rgba(79, 195, 247, 0.2)" : "rgba(2, 136, 209, 0.2)";

        var html = new StringBuilder();
        html.append("<!DOCTYPE html>");
        html.append("<html>");
        html.append("<head>");
        html.append("<meta charset=\"UTF-8\">");
        html.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">");
        html.append("<style>");
        html.append("* { margin: 0; padding: 0; box-sizing: border-box; }");
        html.append("body {");
        html.append(
            "    font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;");
        html.append("    background-color: ").append(backgroundColor).append(";");
        html.append("    color: ").append(textColor).append(";");
        html.append("    display: flex;");
        html.append("    flex-direction: column;");
        html.append("    justify-content: center;");
        html.append("    align-items: center;");
        html.append("    min-height: 100vh;");
        html.append("    margin: 0;");
        html.append("}");
        html.append(".container {");
        html.append("    text-align: center;");
        html.append("    padding: 40px;");
        html.append("}");
        html.append(".spinner {");
        html.append("    width: 50px;");
        html.append("    height: 50px;");
        html.append("    border: 4px solid ").append(spinnerBg).append(";");
        html.append("    border-top-color: ").append(spinnerColor).append(";");
        html.append("    border-radius: 50%;");
        html.append("    animation: spin 1s linear infinite;");
        html.append("    margin: 0 auto 24px;");
        html.append("}");
        html.append("@keyframes spin {");
        html.append("    to { transform: rotate(360deg); }");
        html.append("}");
        html.append("h1 {");
        html.append("    font-size: 28px;");
        html.append("    font-weight: 600;");
        html.append("    margin-bottom: 12px;");
        html.append("    letter-spacing: -0.5px;");
        html.append("}");
        html.append(".message {");
        html.append("    font-size: 16px;");
        html.append("    opacity: 0.7;");
        html.append("}");
        html.append("</style>");
        html.append("</head>");
        html.append("<body>");
        html.append("<div class=\"container\">");
        html.append("<div class=\"spinner\"></div>");
        html.append("<h1>").append(title).append("</h1>");
        html.append("<div class=\"message\">").append(message).append("</div>");
        html.append("</div>");
        html.append("</body>");
        html.append("</html>");

        return html.toString();
    }

    private static Path getUserDataDirectory()
    {
        return Path.of(ConfigurationScope.INSTANCE.getLocation()
            .addTrailingSeparator()
            .append(AI_CHAT_DIR)
            .toFile()
            .getAbsolutePath(), INSTANCE_ID);
    }

    @SuppressWarnings("nls")
    private static void registerCleanupHook(Path userDataDirectory)
    {
        if (!CLEANUP_REGISTERED.compareAndSet(false, true))
        {
            return;
        }
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try (var stream = Files.walk(userDataDirectory))
            {
                stream.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
            catch (IOException | RuntimeException ignored)
            {
                // best-effort cleanup
            }
        }, "ai-chat-userdata-cleanup"));
    }

    private void chatInJob(Optional<AIContext> ctx, IChatAction chatAction)
    {
        ensureWebViewExists();
        var job = dispatcher.createJob(Messages.ChatInteractionJobName, jobCtx -> chat(ctx, chatAction), false,
            CancellationTokens.NONE);
        job.setPriority(Job.INTERACTIVE);
        job.schedule();
    }

    @SuppressWarnings("nls")
    private synchronized IStatus chat(Optional<AIContext> ctx, IChatAction chatAction)
    {
        try
        {
            var chatUrl = settings.getChatUrl();
            var newChatKey = new ChatKey(chatUrl, settings.getClientToken());
            if (!Objects.equals(lastChatKey, newChatKey))
            {
                handler.reset();
                lastChatKey = newChatKey;
                dispatcher.dispatch(() -> {
                    var webEngine = getEngine();
                    return initialize(webEngine, () -> {
                        webEngine.load(chatUrl.toString());
                    });
                }).get().join();

                wink(32);
            }

            chatAction.run();
        }
        catch (Throwable error)
        {
            return Status.warning(AI_CHAT + ": " + error.getMessage(), error);
        }

        return Status.OK_STATUS;
    }

    @SuppressWarnings("nls")
    private CompletableFuture<Boolean> initialize(WebEngine webEngine, Runnable loader)
    {
        var worker = webEngine.getLoadWorker();
        log.trace(TracingSources.CHAT, AI_CHAT, () -> "user agent: " + webEngine.getUserAgent());
        var result = new CompletableFuture<Boolean>();
        ChangeListener<State> stateListener = (observable, oldValue, newValue) -> {
            log.trace(TracingSources.CHAT, AI_CHAT, () -> "new state: " + newValue);
            switch (newValue)
            {
            case SUCCEEDED:
            case FAILED:
            case CANCELLED:
                cleanupInitializationListeners(worker);
                result.complete(newValue == State.SUCCEEDED);
                break;

            default:
                break;
            }
        };

        synchronized (initializationListeners)
        {
            initializationListeners.add(stateListener);
            worker.stateProperty().addListener(stateListener);
        }
        loader.run();
        return result.orTimeout(settings.getTimeout().toNanos(), TimeUnit.NANOSECONDS).exceptionally(error -> {
            cleanupInitializationListeners(worker);
            log.trace(TracingSources.CHAT, "API error", () -> error.toString()); //$NON-NLS-1$
            return false;
        });
    }

    private void cleanupInitializationListeners(javafx.concurrent.Worker<?> worker)
    {
        synchronized (initializationListeners)
        {
            initializationListeners.forEach(listener -> worker.stateProperty().removeListener(listener));
            initializationListeners.clear();
        }
    }

    @SuppressWarnings("nls")
    private void wink(int attempts)
    {
        if (handler.isReady())
        {
            return;
        }

        var tools = mcpTools.getSpecifications().join().stream().map(i -> i.function).collect(Collectors.toList());
        var toolsJson = json.serialize(tools);

        var webEngine = getEngine();
        while (true)
        {
            executeWink(webEngine, toolsJson);

            if (handler.isReady() || attempts-- == 0)
            {
                break;
            }

            try
            {
                Thread.sleep(settings.getMinRequestDelay().toMillis());
            }
            catch (InterruptedException error)
            {
                Thread.currentThread().interrupt();
                log.warning(AI_CHAT, () -> "Wink loop interrupted");
                break;
            }
        }
    }

    @SuppressWarnings("nls")
    private void executeWink(WebEngine webEngine, String toolsJson)
    {
        dispatcher.dispatch(() -> {
            try
            {
                var window = (JSObject)webEngine.executeScript(WINDOW);
                if (window != null)
                {
                    window.setMember(IDE_API, handler);
                    log.trace(TracingSources.CHAT, AI_CHAT, () -> "set callback handler " + window.getMember(IDE_API));
                    var winkScript = String.format(CHAT_API_WINK_TEMPLATE, settings.getClientToken(),
                        settings.getClientUniqueId(), settings.getLanguage(), settings.getTheme());
                    log.trace(TracingSources.CHAT, AI_CHAT, () -> "wink script: " + winkScript);
                    var winkResult = webEngine.executeScript(winkScript);
                    log.trace(TracingSources.CHAT, AI_CHAT,
                        () -> "wink script executed, winked: " + handler.isReady() + ", result: " + winkResult);
                    webEngine.executeScript("if (typeof window.chatApi['set_tools'] === 'function') { "
                        + WINDOW_CHAT_API_SET_TOOLS + javaScript.escape(toolsJson, EMPTY_STRING) + "); }");
                }
                else
                {
                    log.warning(AI_CHAT, () -> "cannot find a chat window");
                }
            }
            catch (Throwable error)
            {
                handler.reset();
                log.logError(error);
            }
        });
    }

    private WebEngine getEngine()
    {
        var webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        return webEngine;
    }

    private Optional<AIContext> getContext(String chatId)
    {
        return Optional.ofNullable(contexts.getIfPresent(chatId));
    }

    private interface IChatAction
    {
        void run();
    }

    private static class ContextInfo
    {
        String title;
        String scriptLanguage;
        String programingLanguage;
    }

    private static class ChatKey
    {
        private final URL url;
        private final String token;

        public ChatKey(URL url, String token)
        {
            this.url = url;
            this.token = token;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(token, url);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ChatKey other = (ChatKey)obj;
            return Objects.equals(token, other.token) && Objects.equals(url, other.url);
        }
    }

    @Override
    public void onServiceStateChange(ServiceState serviceState)
    {
        // Skip first settings change
        if (!isFirst && serviceState == ServiceState.SETTINGS_CHANGED)
        {
            isFirst = false;
            reset();
            warmUp();
        }
    }

    @Override
    public void onActionStateChange(ActionState actionState)
    {
        // Do nothing
    }

    @Override
    public void hide()
    {
        reset();
        warmUp();
    }

    private void warmUp()
    {
        chatInJob(Optional.empty(), () -> { /* warming up */
        });
    }

    private void reset()
    {
        contexts.invalidateAll();
        lastChatKey = null;
    }
}
