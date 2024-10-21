/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringEscapeUtils;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.assistent.IParametersService;
import org.e1c.edt.ai.assistent.ISettingsTracker;
import org.e1c.edt.ai.assistent.ParametersService;
import org.e1c.edt.ai.assistent.model.Parameters;
import org.e1c.edt.ai.client.AISettings;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.ConfigurationScope;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.event.EventHandler;
import javafx.scene.control.ScrollPane;
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
    private static final String CHAT_API_WINK_TEMPLATE = "window.chatApi.wink({client_id: \"%s\", client_uid: \"%s\"})"; //$NON-NLS-1$
    private static final String IDE_API = "ideApi"; //$NON-NLS-1$

    private final ILog log;
    private final ISettingsProvider settingsProvider;
    private final IUI ui;
    private final IDispatcher dispatcher;
    private final IdeApiHandler handler;
    private final IParametersService parametersService;
    private final ISettingsTracker settingsTracker;
    private WebView webView;
    private String lastChatUrl;
    private CompletableFuture<Boolean> initializing = CompletableFuture.completedFuture(true);

    @Inject
    public Chat(ILog log, ISettingsProvider settingsProvider, IUI ui, IDispatcher dispatcher,
        IdeApiHandler handler, IParametersService parametersService, ISettingsTracker settingsTracker)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settingsProvider);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(handler);
        Preconditions.checkNotNull(parametersService);
        Preconditions.checkNotNull(settingsTracker);
        this.log = log;
        this.settingsProvider = settingsProvider;
        this.ui = ui;
        this.dispatcher = dispatcher;
        this.handler = handler;
        this.parametersService = parametersService;
        this.settingsTracker = settingsTracker;
    }

    @Override
    public void reviewCode(String codeSnippet)
    {
        Preconditions.checkNotNull(codeSnippet);
        chat("review_code", codeSnippet, null); //$NON-NLS-1$
    }

    @Override
    public void explainCode(String codeSnippet)
    {
        Preconditions.checkNotNull(codeSnippet);
        chat("comment_code", codeSnippet, null); //$NON-NLS-1$
    }

    @Override
    public void fixCode(String codeSnippet, String details)
    {
        Preconditions.checkNotNull(codeSnippet);
        chat("fix_code", codeSnippet, details); //$NON-NLS-1$
    }

    @Override
    public void generateDocComments(String method)
    {
        Preconditions.checkNotNull(method);
        chat("document_code", method, null); //$NON-NLS-1$
    }

    @Override
    public void askQuestion(String userQuestion)
    {
        Preconditions.checkNotNull(userQuestion);
        chat("plain_message", userQuestion, null); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void chat(String topic, String subject, String details)
    {
        dispatcher.dispatch(() -> ui.showView(ChatView.ID));
        chatInJob(() -> {
            var script = new StringBuilder();
            script.append("window.chatApi.");
            script.append(topic);
            script.append("(`");
            script.append(StringEscapeUtils.escapeJavaScript(subject));
            if (details != null && !details.isBlank())
            {
                script.append("`, `");
                script.append(StringEscapeUtils.escapeJavaScript(details));
            }

            script.append("`)");
            var scriptText = script.toString();
            log.trace(AI_CHAT, "executing script: " + scriptText);
            dispatcher.dispatch(() -> webView.getEngine().executeScript(scriptText));
            log.trace(AI_CHAT, "script executed");
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

        chatInJob(() -> {
            /**/ });
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

        var webEngine = view.getEngine();
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
                log.trace(AI_CHAT, "is running: " + newValue);
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

    @SuppressWarnings("nls")
    private void chatInJob(Runnable chatAction)
    {
        ensureWebViewExists();
        new Job(Messages.ChatInteractionJobName)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                Optional<Parameters> parameters;
                try
                {
                    parameters = parametersService.getParametersAsync().get();
                }
                catch (InterruptedException | ExecutionException e)
                {
                    log.logError(e);
                    return Status.error(e.getMessage());
                }

                var settings = settingsProvider.getSettings();
                if (parameters.isEmpty() || settings.isEmpty())
                {
                    log.logError("Failed to get the parameters for chat.");
                    return Status.error("Failed to get the parameters.");
                }

                var chatUrl = parameters.get().chatUrl;
                var reset = settingsTracker.register(ParametersService.class.getName(), settings);
                dispatcher.dispatch(() -> {
                    if (lastChatUrl != chatUrl || reset)
                    {
                        lastChatUrl = chatUrl;
                        initializing =
                            initialize(() -> webView.getEngine().load(lastChatUrl), () -> wink(settings.get()), 10000);
                    }
                });

                final var statuses = new ArrayList<IStatus>();
                initializing.whenComplete((r, e) -> {
                    if (e == null)
                    {
                        chatAction.run();
                    }
                    else
                    {
                        statuses.add(Status.error(e.getMessage(), e));
                        lastChatUrl = null;
                    }
                }).join();

                return statuses.size() == 0 ? Status.OK_STATUS : statuses.get(0);
            }
        }.schedule();
    }

    @SuppressWarnings("nls")
    private void wink(AISettings settings)
    {
        try
        {
            var winkScript =
                String.format(CHAT_API_WINK_TEMPLATE, settings.getClientToken(), settings.getClientUniqueId());
            log.trace(AI_CHAT, "wink script: " + winkScript); //$NON-NLS-1$
            webView.getEngine().executeScript(winkScript);
            log.trace(AI_CHAT, "wink script executed");
        }
        catch (Throwable error)
        {
            log.logError(error);
        }
    }

    @SuppressWarnings("nls")
    private CompletableFuture<Boolean> initialize(Runnable loader, Runnable initializer, int timeout)
    {
        var webEngine = webView.getEngine();
        webEngine.setJavaScriptEnabled(true);
        log.trace(AI_CHAT, "user agent: " + webEngine.getUserAgent());
        var result = new CompletableFuture<Boolean>();
        var stateListener = new ChangeListener<State>()
        {
            @Override
            public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue)
            {
                log.trace(AI_CHAT, "new state: " + newValue);
                switch (newValue)
                {
                case SUCCEEDED:
                    var window = (JSObject)webEngine.executeScript("window"); //$NON-NLS-1$
                    window.setMember(IDE_API, handler);
                    initializer.run();
                    result.complete(true);
                    break;

                default:
                    break;
                }
            }
        };

        var worker = webEngine.getLoadWorker();
        worker.stateProperty().addListener(stateListener);
        loader.run();
        return result.orTimeout(timeout, TimeUnit.MILLISECONDS)
            .whenComplete((r, e) -> worker.stateProperty().removeListener(stateListener));
    }
}
