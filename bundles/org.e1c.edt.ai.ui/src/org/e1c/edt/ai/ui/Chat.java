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
    private static final String CHAT_API_WINK_TEMPLATE = "window.chatApi.wink({client_id: \"%s\", client_uid: \"%s\"})"; //$NON-NLS-1$
    private static final String IDE_API = "ideApi"; //$NON-NLS-1$
    private static final String WELCOME_PAGE_TITLE = "Welcome page"; //$NON-NLS-1$

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
        chat("review_code", codeSnippet); //$NON-NLS-1$
    }

    @Override
    public void explainCode(String codeSnippet)
    {
        Preconditions.checkNotNull(codeSnippet);
        chat("comment_code", codeSnippet); //$NON-NLS-1$
    }

    @Override
    public void fixCode(String codeSnippet)
    {
        Preconditions.checkNotNull(codeSnippet);
        chat("fix_code", codeSnippet); //$NON-NLS-1$
    }

    @Override
    public void generateDocComments(String method)
    {
        Preconditions.checkNotNull(method);
        chat("document_code", method); //$NON-NLS-1$
    }

    @Override
    public void askQuestion(String userQuestion)
    {
        Preconditions.checkNotNull(userQuestion);
        chat("plain_message", userQuestion); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void chat(String topic, String subject)
    {
        ui.showView(ChatView.ID);
        chatInJob(view -> {
            var script = "window.chatApi." + topic + "(`" + StringEscapeUtils.escapeJavaScript(subject) + "`)";
            view.getEngine().executeScript(script);
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
                    return Status.error(e.getMessage());
                }

                if (!parameters.isPresent())
                {
                    return Status.error("Failed to get the parameters."); //$NON-NLS-1$
                }

                dispatcher
                    .dispatch(() -> webView = webView.or(() -> Optional.of(createWebView(parameters.get().chatUrl))));
                if (webView.isEmpty())
                {
                    return Status.error("Failed to get the parameters."); //$NON-NLS-1$
                }

                webView.get().thenAcceptAsync(view -> dispatcher.dispatch(() -> consumer.accept(view)));
                return Status.OK_STATUS;
            }
        }.schedule();
    }

    private CompletableFuture<WebView> createWebView(String chatUrl)
    {
        var view = new WebView();
        view.setLayoutX(-1);
        view.setLayoutY(-1);
        WebEngine webEngine = view.getEngine();
        var result = new CompletableFuture<WebView>();
        webEngine.titleProperty().addListener(new ChangeListener<String>()
        {
            @Override
            public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
            {
                if (newValue.contains(WELCOME_PAGE_TITLE))
                {
                    // initialize only once, no need this listener anymore
                    webEngine.titleProperty().removeListener(this);

                    JSObject window = (JSObject)webEngine.executeScript("window"); //$NON-NLS-1$
                    window.setMember(IDE_API, handler);
                    settingsProvider.getSettings()
                        .ifPresent(settings -> webEngine.executeScript(String.format(CHAT_API_WINK_TEMPLATE,
                            settings.getClientToken(), settings.getClientUniqueId())));
                    result.complete(view);
                }
            }
        });

        webEngine.load(chatUrl);
        return result;
    }
}
