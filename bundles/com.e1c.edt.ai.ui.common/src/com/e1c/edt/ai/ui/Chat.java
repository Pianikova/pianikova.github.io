/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.core.runtime.IProgressMonitor;
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
import com.e1c.edt.ai.IContextEntities;
import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILocalContext;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.assistent.ISessionService;
import com.e1c.edt.ai.assistent.ISettingsTracker;
import com.e1c.edt.ai.assistent.IStateService;
import com.e1c.edt.ai.assistent.model.ChatContext;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.EventHandler;
import javafx.scene.control.ScrollPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebErrorEvent;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * @author George Suaridze
 *
 */
public class Chat implements IChat, IChatDialog
{
    private static final String AI_CHAT_DIR = "ai.chat"; //$NON-NLS-1$
    private static final String AI_CHAT = "AI Chat"; //$NON-NLS-1$
    private static final String CHAT_API_WINK_TEMPLATE =
        "window.chatApi.wink({client_id: \"%s\", client_uid: \"%s\"}, \"%s\", \"%s\")"; //$NON-NLS-1$
    private static final String IDE_API = "ideApi"; //$NON-NLS-1$
    private static final String EMPTY_STRING = "``"; //$NON-NLS-1$
    private static final String NULL_VALUE = "null"; //$NON-NLS-1$
    private static final Character ARGS_SEPARATOR = ',';

    private final ILog log;
    private final ISettings settings;
    private final IUI ui;
    private final IDispatcher dispatcher;
    private final IdeApiHandler handler;
    private final ISettingsTracker settingsTracker;
    private final IContextEntities contextEntities;
    private final IJavaScript javaScript;
    private final IStateService stateService;
    private final ISessionService sessionService;
    private final IModuleNameProvider moduleNameProvider;
    private final IFileSystem fileSystem;
    private final ILocalContext localContext;
    private final IProposalsProvider proposalsProvider;
    private final IJson json;
    private WebView webView;
    private URL lastChatUrl;
    private CompletableFuture<Boolean> initializing = CompletableFuture.completedFuture(true);
    private String lastDialogPath;

    @Inject
    public Chat(ILog log, ISettings settings, IUI ui, IDispatcher dispatcher, IdeApiHandler handler,
        ISettingsTracker settingsTracker, IContextEntities contextEntities, IJavaScript javaScript,
        IStateService stateService, ISessionService sessionService, IModuleNameProvider moduleNameProvider,
        IFileSystem fileSystem, ILocalContext localContext, IProposalsProvider proposalsProvider, IJson json)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(handler);
        Preconditions.checkNotNull(settingsTracker);
        Preconditions.checkNotNull(contextEntities);
        Preconditions.checkNotNull(javaScript);
        Preconditions.checkNotNull(stateService);
        Preconditions.checkNotNull(sessionService);
        Preconditions.checkNotNull(moduleNameProvider);
        Preconditions.checkNotNull(fileSystem);
        Preconditions.checkNotNull(localContext);
        Preconditions.checkNotNull(proposalsProvider);
        Preconditions.checkNotNull(json);
        this.log = log;
        this.settings = settings;
        this.ui = ui;
        this.dispatcher = dispatcher;
        this.handler = handler;
        this.settingsTracker = settingsTracker;
        this.contextEntities = contextEntities;
        this.javaScript = javaScript;
        this.stateService = stateService;
        this.sessionService = sessionService;
        this.moduleNameProvider = moduleNameProvider;
        this.fileSystem = fileSystem;
        this.localContext = localContext;
        this.proposalsProvider = proposalsProvider;
        this.json = json;
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
        chat("insert_code", codeSnippet, null, ctx); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    @Override
    public void addFile()
    {
        var shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
        var dialog = new FileDialog(shell, SWT.OPEN);
        dialog.setFilterPath(lastDialogPath);
        var file = dialog.open();
        if (file == null)
        {
            return;
        }

        lastDialogPath = dialog.getFilterPath();
        var filePath = Path.of(file);
        if (!fileSystem.isTextFile(filePath))
        {
            MessageDialog.openError(shell, Messages.ErrorReadingFile, Messages.OnlyTextFilesSupported);
            return;
        }

        var optionalContent = fileSystem.getText(filePath);
        if (optionalContent.isEmpty())
        {
            MessageDialog.openWarning(shell, Messages.ErrorReadingFile, Messages.UnableToReadFile);
            return;
        }

        var content = optionalContent.get();
        var ctx = new AIContext(new ProjectId(lastDialogPath, null), filePath.getFileName().toString(), null);
        chat("insert_code", content, null, ctx);
    }

    @SuppressWarnings({ "nls", "restriction" })
    private void chat(String topic, String subject, String details, AIContext ctx)
    {
        ui.showView(BaseChatView.ID);
        chatInJob(Optional.ofNullable(ctx), (optionalSessionId) -> {
            stateService.setState(Chat.class.getName(), ActionState.BUSY);
            try {
                String scriptLanguage = null;
                String programingLanguage = null;
                var title = NULL_VALUE;
                if (ctx != null)
                {
                    title = moduleNameProvider.getModuleName(ctx.getPath()).orElseGet(() -> ctx.getPath());
                    var chatContext = new ChatContext();
                    var doc = ctx.getDocument();
                    contextEntities.fill(ctx, chatContext, IStatistics.Empty, CancellationTokens.NONE);
                    scriptLanguage = chatContext.scriptLanguage;
                    programingLanguage = chatContext.programingLanguage;
                    if (doc instanceof DiffDocument)
                    {
                        programingLanguage = "git diff";
                    }
                }

                var script = new StringBuilder();
                script.append("window.chatApi.");
                script.append(topic);
                script.append('(');
                script.append(javaScript.escape(subject, EMPTY_STRING));
                script.append(ARGS_SEPARATOR);
                script.append(javaScript.escape(scriptLanguage, EMPTY_STRING));
                script.append(ARGS_SEPARATOR);
                script.append(javaScript.escape(programingLanguage, EMPTY_STRING));
                if (details != null)
                {
                    script.append(ARGS_SEPARATOR);
                    script.append(javaScript.escape(details, EMPTY_STRING));
                }

                script.append(ARGS_SEPARATOR);
                script
                    .append(javaScript.escape(Optional.ofNullable(ctx).map(i -> i.getPath()).orElse(null), NULL_VALUE));
                if (topic.equals("insert_code"))
                {
                    var document = ctx.getDocument();
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

                script.append(ARGS_SEPARATOR);
                script.append(javaScript.escape(optionalSessionId.orElse(null), NULL_VALUE));

                final var curTitle = title;
                log.debug(AI_CHAT, () -> "title: " + curTitle);
                script.append(ARGS_SEPARATOR);
                script.append(javaScript.escape(curTitle, NULL_VALUE));

                var context = localContext.create(ctx, IStatistics.Empty, CancellationTokens.NONE);
                var sourceViewer = ui.getLastSourceViewer();
                if (sourceViewer.isPresent())
                {
                    context.proposals =
                        proposalsProvider.getProposals(ctx, sourceViewer.get(), 600, CancellationTokens.NONE)
                            .orElse(null);
                }

                var contextJson = json.serialize(context);
                script.append(ARGS_SEPARATOR);
                script.append(javaScript.escape(contextJson, NULL_VALUE));

                script.append(')');
                var scriptText = script.toString();
                dispatcher.dispatchAsync(() -> {
                    log.debug(AI_CHAT, () -> "executing script: " + scriptText);
                    getEgine().executeScript(scriptText);
                    log.trace(AI_CHAT, () -> "script executed");
                });
            }
            finally
            {
                stateService.setState(Chat.class.getName(), ActionState.INACTIVE);
            }
        });
    }

    @Override
    public void show(ScrollPane pane)
    {
        ensureWebViewExists();
        pane.setContent(webView);
        webView.setFocusTraversable(true);
        webView.setPrefWidth(pane.getWidth());
        webView.setPrefHeight(pane.getHeight());
        pane.widthProperty().addListener(new ChangeListener<Object>()
        {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue)
            {
                Double width = (Double)newValue;
                webView.setPrefWidth(width);
            }
        });

        pane.heightProperty().addListener(new ChangeListener<Object>()
        {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue)
            {
                Double height = (Double)newValue;
                webView.setPrefHeight(height);
            }
        });

        chatInJob(Optional.empty(), optionalSessionId -> {
            /**/ });
    }

    private void ensureWebViewExists()
    {
        if (webView != null)
        {
            if (webView.isVisible())
            {
                webView.setVisible(true);
            }

            return;
        }

        var view = new WebView();
        webView = view;
        view.setLayoutX(-1);
        view.setLayoutY(-1);

        var webEngine = getEgine();
        webEngine.setUserDataDirectory(getUserDataDirectory().toFile());
        webEngine.setOnError(new EventHandler<WebErrorEvent>()
        {
            @Override
            public void handle(WebErrorEvent event)
            {
                log.logError(event.getMessage());
                log.logError(event.getException());
            }
        });

        var worker = webEngine.getLoadWorker();
        worker.runningProperty().addListener(new ChangeListener<Boolean>()
        {
            @SuppressWarnings("nls")
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                log.debug(AI_CHAT, () -> "is running: " + newValue);
            }
        });
    }

    private static Path getUserDataDirectory()
    {
        return Path.of(ConfigurationScope.INSTANCE.getLocation()
            .addTrailingSeparator()
            .append(AI_CHAT_DIR)
            .toFile()
            .getAbsolutePath());
    }

    private void chatInJob(Optional<AIContext> ctx, IChatAction chatAction)
    {
        ensureWebViewExists();
        new Job(Messages.ChatInteractionJobName)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                return chat(ctx, chatAction);
            }
        }.schedule();
    }

    @SuppressWarnings("nls")
    private synchronized IStatus chat(Optional<AIContext> ctx, IChatAction chatAction)
    {
        try
        {
            Optional<String> sessionId = Optional.empty();
            if (ctx.isPresent())
            {
                sessionId = sessionService.getSessionAsync(ctx.get().getProjectId()).get().map(i -> i.sessionId);
            }

            var chatUrl = settings.getChatUrl();
            var reset = settingsTracker.register(Chat.class.getName(), settings);
            if (lastChatUrl != chatUrl || reset)
            {
                lastChatUrl = chatUrl;
                initializing = dispatcher.dispatch(() -> {
                    var webEngine = getEgine();
                    return initialize(webEngine, () -> webEngine.load(lastChatUrl.toString()));
                }).get();
            }

            initializing.get();
            wink(32);
            chatAction.run(sessionId);
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
        log.trace(AI_CHAT, () -> "user agent: " + webEngine.getUserAgent());
        var result = new CompletableFuture<Boolean>();
        var listeners = new ArrayList<ChangeListener<State>>();
        var stateListener = new ChangeListener<State>()
        {
            @Override
            public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue)
            {
                log.debug(AI_CHAT, () -> "new state: " + newValue);
                switch (newValue)
                {
                case SUCCEEDED:
                    for (var listener : listeners)
                    {
                        worker.stateProperty().removeListener(listener);
                    }

                    listeners.clear();
                    result.complete(true);
                    break;

                default:
                    break;
                }
            }
        };


        listeners.add(stateListener);
        worker.stateProperty().addListener(stateListener);
        loader.run();
        return result.orTimeout(settings.getTimeout().toNanos(), TimeUnit.NANOSECONDS).exceptionally(error -> {
            log.logError(error);
            return false;
        });
    }

    @SuppressWarnings("nls")
    private void wink(int attempts)
    {
        var webEngine = getEgine();
        while (true)
        {
            dispatcher.dispatch(() -> {
                try
                {
                    var window = (JSObject)webEngine.executeScript("window");
                    if (window != null)
                    {
                        window.setMember(IDE_API, handler);
                        log.debug(AI_CHAT, () -> "set callback handler " + window.getMember(IDE_API));
                        var winkScript = String.format(CHAT_API_WINK_TEMPLATE, settings.getClientToken(),
                            settings.getClientUniqueId(), settings.getLanguage(), settings.getTheme());
                        log.debug(AI_CHAT, () -> "wink script: " + winkScript);
                        webEngine.executeScript(winkScript);
                        log.trace(AI_CHAT, () -> "wink script executed, winked: " + handler.isReady());
                    }
                    else
                    {
                        log.warning(AI_CHAT, () -> "cannot find a chat window");
                    }
                }
                catch (Throwable error)
                {
                    log.logError(error);
                }
            });

            if (handler.isReady() || attempts-- == 0)
            {
                break;
            }

            try
            {
                Thread.sleep(settings.getMinRequestDelay().toMillis());
            }
            catch (Throwable error)
            {
                //
            }
        }
    }

    private WebEngine getEgine()
    {
        var webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        return webEngine;
    }

    private interface IChatAction
    {
        void run(Optional<String> sessionId);
    }
}
