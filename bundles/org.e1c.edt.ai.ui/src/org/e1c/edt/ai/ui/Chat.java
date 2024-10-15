/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.apache.commons.lang.StringEscapeUtils;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.assistent.IParametersService;
import org.e1c.edt.ai.assistent.model.Parameters;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.control.ScrollPane;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

/**
 * @author George Suaridze
 *
 */
public class Chat implements IChat, IChatDialog
{
    private static final String AI_CHAT = "AI Chat"; //$NON-NLS-1$
    private static final String CHAT_API_WINK_TEMPLATE = "window.chatApi.wink({client_id: \"%s\", client_uid: \"%s\"})"; //$NON-NLS-1$
    private static final String IDE_API = "ideApi"; //$NON-NLS-1$

    private final ILog log;
    private final ISettingsProvider settingsProvider;
    private final IUI ui;
    private final IDispatcher dispatcher;
    private final IdeApiHandler handler;
    private final IParametersService parametersService;
    private Optional<CompletableFuture<WebView>> webView = Optional.empty();

    @Inject
    public Chat(ILog log, ISettingsProvider settingsProvider, IUI ui, IDispatcher dispatcher,
        IdeApiHandler handler, IParametersService parametersService)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settingsProvider);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(handler);
        Preconditions.checkNotNull(parametersService);
        this.log = log;
        this.settingsProvider = settingsProvider;
        this.ui = ui;
        this.dispatcher = dispatcher;
        this.handler = handler;
        this.parametersService = parametersService;
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
        ui.showView(ChatView.ID);
        chatInJob(view -> {
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
            view.getEngine().executeScript(scriptText);
            log.trace(AI_CHAT, "script executed");
        });
    }

    @Override
    public void show(ScrollPane pane)
    {
        chatInJob(view -> {
            pane.setContent(view);
            view.setFocusTraversable(true);
            view.setPrefWidth(pane.getWidth());
            view.setPrefHeight(pane.getHeight());
            pane.widthProperty().addListener(new ChangeListener<Object>()
            {
                @Override
                public void changed(ObservableValue<?> observable, Object oldValue, Object newValue)
                {
                    Double width = (Double)newValue;
                    view.setPrefWidth(width);
                }
            });

            pane.heightProperty().addListener(new ChangeListener<Object>()
            {
                @Override
                public void changed(ObservableValue<?> observable, Object oldValue, Object newValue)
                {
                    Double height = (Double)newValue;
                    view.setPrefHeight(height);
                }
            });
        });
    }

    private void chatInJob(Consumer<WebView> consumer)
    {
        new Job(Messages.ChatInteractionJobName)
        {
            @SuppressWarnings("nls")
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

                if (!parameters.isPresent())
                {
                    log.logError("Failed to get the parameters for chat.");
                    return Status.error("Failed to get the parameters.");
                }

                dispatcher
                    .dispatch(() -> webView = webView.or(() -> Optional.of(createWebView(parameters.get().chatUrl))));
                if (webView.isEmpty())
                {
                    log.logError("Failed to create chat web view.");
                    return Status.error("Failed to create web view.");
                }

                webView.get().thenAcceptAsync(view -> dispatcher.dispatchAsync(() -> consumer.accept(view)));
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    @SuppressWarnings("nls")
    private CompletableFuture<WebView> createWebView(String chatUrl)
    {
        var view = new WebView();
        view.setLayoutX(-1);
        view.setLayoutY(-1);
        WebEngine webEngine = view.getEngine();
        var result = new CompletableFuture<WebView>();
        var worker = webEngine.getLoadWorker();
        worker.stateProperty().addListener(new ChangeListener<State>()
        {
            @Override
            public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue)
            {
                log.trace(AI_CHAT, "new state: " + newValue);
                switch (newValue)
                {
                case SUCCEEDED:
                    JSObject window = (JSObject)webEngine.executeScript("window"); //$NON-NLS-1$
                    window.setMember(IDE_API, handler);
                    settingsProvider.getSettings()
                        .ifPresent(settings -> {
                            try
                            {
                                webEngine.executeScript(String.format(CHAT_API_WINK_TEMPLATE, settings.getClientToken(),
                                    settings.getClientUniqueId()));
                            }
                            catch (Throwable error)
                            {
                                log.logError(error);
                            }
                        });
                    result.complete(view);
                    break;

                default:
                    break;
                }
            }
        });

        worker.runningProperty().addListener(new ChangeListener<Boolean>()
        {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean newValue)
            {
                log.trace(AI_CHAT, "is running: " + newValue);
            }
        });

        log.trace(AI_CHAT, "loading...");
        webEngine.load(chatUrl);
        return result;
    }
}
