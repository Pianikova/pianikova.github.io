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

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IContextEntities;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettingsProvider;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.IUISettings;
import com.e1c.edt.ai.assistent.IParametersService;
import com.e1c.edt.ai.assistent.ISessionService;
import com.e1c.edt.ai.assistent.ISettingsTracker;
import com.e1c.edt.ai.assistent.IStateService;
import com.e1c.edt.ai.assistent.model.ChatContext;
import com.e1c.edt.ai.client.AISettings;
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

    private final ILog log;
    private final ISettingsProvider settingsProvider;
    private final IUI ui;
    private final IDispatcher dispatcher;
    private final IdeApiHandler handler;
    private final IParametersService parametersService;
    private final ISettingsTracker settingsTracker;
    private final IUISettings uiSettings;
    private final IContextEntities contextEntities;
    private final IJavaScript javaScript;
    private final IStateService stateService;
    private final ISessionService sessionService;
    private WebView webView;
    private URL lastChatUrl;
    private CompletableFuture<Boolean> initializing = CompletableFuture.completedFuture(true);

    @Inject
    public Chat(ILog log, ISettingsProvider settingsProvider, IUI ui, IDispatcher dispatcher,
        IdeApiHandler handler, IParametersService parametersService, ISettingsTracker settingsTracker,
        IUISettings uiSettings, IContextEntities contextEntities, IJavaScript javaScript, IStateService stateService,
        ISessionService sessionService)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settingsProvider);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(handler);
        Preconditions.checkNotNull(parametersService);
        Preconditions.checkNotNull(settingsTracker);
        Preconditions.checkNotNull(uiSettings);
        Preconditions.checkNotNull(contextEntities);
        Preconditions.checkNotNull(javaScript);
        Preconditions.checkNotNull(stateService);
        Preconditions.checkNotNull(sessionService);
        this.log = log;
        this.settingsProvider = settingsProvider;
        this.ui = ui;
        this.dispatcher = dispatcher;
        this.handler = handler;
        this.parametersService = parametersService;
        this.settingsTracker = settingsTracker;
        this.uiSettings = uiSettings;
        this.contextEntities = contextEntities;
        this.javaScript = javaScript;
        this.stateService = stateService;
        this.sessionService = sessionService;
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

    @SuppressWarnings("nls")
    private void chat(String topic, String subject, String details, AIContext ctx)
    {
        ui.showView(BaseChatView.ID);
        chatInJob(Optional.ofNullable(ctx), (settings, optionalSessionId) -> {
            stateService.setState(Chat.class.getName(), ActionState.BUSY);
            try {
                String scriptLanguage = null;
                String programingLanguage = null;
                if (ctx != null)
                {
                    var chatContext = new ChatContext();
                    contextEntities.fill(ctx, chatContext, IStatistics.Empty, CancellationTokens.NONE);
                    scriptLanguage = chatContext.scriptLanguage;
                    programingLanguage = chatContext.programingLanguage;
                }

                var settingsScriptLanuage = settings.getLlmParameters().scriptLanguage;
                if (settingsScriptLanuage != null && !settingsScriptLanuage.isBlank())
                {
                    scriptLanguage = settingsScriptLanuage;
                }

                var script = new StringBuilder();
                script.append("window.chatApi.");
                script.append(topic);
                script.append("(`");
                script.append(javaScript.escape(subject));
                script.append("`, `");

                if (scriptLanguage != null)
                {
                    script.append(scriptLanguage);
                }

                script.append("`, `");
                if (programingLanguage != null)
                {
                    script.append(programingLanguage);
                }

                if (details != null)
                {
                    script.append("`, `");
                    script.append(javaScript.escape(details));
                }

                // TODO: use optionalSessionId

                script.append("`)");
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

        chatInJob(Optional.empty(), (settings, optionalSessionId) -> {
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
            var parameters = parametersService.getParametersAsync().get();
            if (parameters.isEmpty())
            {
                return Status.OK_STATUS;
            }

            var chatUrl = parameters.get().chatUrl;
            var settings = settingsProvider.getSettings();
            var reset = settingsTracker.register(IParametersService.class.getName(), settings);
            if (lastChatUrl != chatUrl || reset)
            {
                lastChatUrl = chatUrl;
                initializing = dispatcher.dispatch(() -> {
                    var webEngine = getEgine();
                    return initialize(webEngine, settings, () -> webEngine.load(lastChatUrl.toString()));
                }).get();
            }

            initializing.get();
            wink(settings, 32);
            Optional<String> sessionId = Optional.empty();
            if (ctx.isPresent())
            {
                sessionId = sessionService.getSessionAsync(ctx.get().getProjectId()).get().map(i -> i.sessionId);
            }

            chatAction.run(settings, sessionId);
        }
        catch (Throwable error)
        {
            return Status.warning(AI_CHAT + ": " + error.getMessage(), error);
        }

        return Status.OK_STATUS;
    }

    @SuppressWarnings("nls")
    private CompletableFuture<Boolean> initialize(WebEngine webEngine, AISettings settings, Runnable loader)
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
        return result.orTimeout(uiSettings.getTimeout().toNanos(), TimeUnit.NANOSECONDS).exceptionally(error -> {
            log.logError(error);
            return false;
        });
    }

    @SuppressWarnings("nls")
    private void wink(AISettings settings, int attempts)
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
                            settings.getClientUniqueId(), uiSettings.getLanguage(), uiSettings.getTheme());
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
                Thread.sleep(uiSettings.getMinRequestDelay().toMillis());
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
        void run(AISettings settings, Optional<String> sessionId);
    }
}
