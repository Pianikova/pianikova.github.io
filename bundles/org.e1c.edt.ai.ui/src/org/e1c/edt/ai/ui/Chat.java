/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.apache.commons.lang.StringEscapeUtils;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.ui.views.ChatView;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

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
    private Optional<WebView> webView = Optional.empty();

    public Chat(ILog log, ISettingsProvider settingsProvider, IUI ui, IDispatcher dispatcher,
        IdeApiHandler handler)
    {
        this.settingsProvider = settingsProvider;
        this.ui = ui;
        this.dispatcher = dispatcher;
        this.handler = handler;
    }

    @Override
    public void reviewCode(String codeSnippet)
    {
        chat("review_code", codeSnippet); //$NON-NLS-1$
    }

    @Override
    public void explainCode(String codeSnippet)
    {
        chat("comment_code", codeSnippet); //$NON-NLS-1$
    }

    @Override
    public void fixCode(String codeSnippet)
    {
        chat("fix_code", codeSnippet); //$NON-NLS-1$
    }

    @Override
    public void generateDocComments(String method)
    {
        chat("document_code", method); //$NON-NLS-1$
    }

    @Override
    public void askQuestion(String userQuestion)
    {
        chat("document_code", userQuestion); //$NON-NLS-1$
    }

    @SuppressWarnings("nls")
    private void chat(String topic, String subject)
    {
        var script = "window.chatApi." + topic + "(`" + StringEscapeUtils.escapeJavaScript(subject) + "`)";
        new Job(Messages.ChatInteractionJobName)
        {
            @Override
            protected IStatus run(IProgressMonitor monitor)
            {
                dispatcher.dispatch(() -> {
                    ui.showView(ChatView.ID).ifPresent(view -> view.setFocus());
                    getWebView().ifPresent(view -> view.getEngine().executeScript(script));
                });

                return Status.OK_STATUS;
            }
        }.schedule();
    }

    @Override
    public void show(ScrollPane pane)
    {
        getWebView().ifPresent(view -> {
            pane.setContent(view);
            view.setFocusTraversable(true);
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

    private Optional<WebView> getWebView()
    {
        return webView = webView.or(() -> dispatcher.dispatch(() -> createWebView()));
    }

    private WebView createWebView()
    {
        var view = new WebView();
        view.setLayoutX(-1);
        view.setLayoutY(-1);
        WebEngine webEngine = view.getEngine();
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
                }
            }
        });

        settingsProvider.getSettings().ifPresent(settings -> webEngine.load(settings.getChatURL().toString()));
        return view;
    }
}
