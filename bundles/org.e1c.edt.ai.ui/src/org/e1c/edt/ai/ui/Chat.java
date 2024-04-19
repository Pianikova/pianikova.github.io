/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;
import java.util.function.Consumer;

import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsProvider;
import org.e1c.edt.ai.ui.views.ChatView;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.ui.IViewPart;

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
    private static final String CHAT_API_WINK = "window.chatApi.wink()"; //$NON-NLS-1$
    private static final String IDE_API = "ideApi"; //$NON-NLS-1$
    private static final String WELCOME_PAGE_TITLE = "Welcome page"; //$NON-NLS-1$
    private static final String CHAT_PAGE_TITLE = "Chat page"; //$NON-NLS-1$

    private final ISettingsProvider settingsProvider;
    private final IUI ui;
    private final IDispatcher dispatcher;
    private final IdeApiHandler handler;
    private Optional<WebView> webView = Optional.empty();

    public Chat(ILog log, ISettingsProvider settingsProvider, IUI ui, IDispatcher dispatcher)
    {
        this.settingsProvider = settingsProvider;
        this.ui = ui;
        this.dispatcher = dispatcher;
        handler = new IdeApiHandler(log, ui);
    }

    @Override
    public void reviewCode(String codeSnippet)
    {
        getEngine().ifPresent(webEngine -> new SendMessageJob(dispatcher, webEngine, codeSnippet, (String code) -> {
            webEngine.executeScript("window.chatApi.review_code(`" + esqapeString(code) + "`)"); //$NON-NLS-1$ //$NON-NLS-2$
        }).schedule(50));
    }

    @Override
    public void explainCode(String codeSnippet)
    {
        getEngine().ifPresent(webEngine -> new SendMessageJob(dispatcher, webEngine, codeSnippet, (String code) -> {
            webEngine.executeScript("window.chatApi.comment_code(`" + esqapeString(code) + "`)"); //$NON-NLS-1$ //$NON-NLS-2$
        }).schedule(50));
    }

    @Override
    public void fixCode(String codeSnippet)
    {
        getEngine().ifPresent(webEngine -> new SendMessageJob(dispatcher, webEngine, codeSnippet, (String code) -> {
            webEngine.executeScript("window.chatApi.fix_code(`" + esqapeString(code) + "`)"); //$NON-NLS-1$ //$NON-NLS-2$
        }).schedule(50));
    }

    @Override
    public void generateDocComments(String method)
    {
        getEngine().ifPresent(webEngine -> new SendMessageJob(dispatcher, webEngine, method, (String code) -> {
            webEngine.executeScript("window.chatApi.document_code(`" + esqapeString(code) + "`)"); //$NON-NLS-1$ //$NON-NLS-2$
        }).schedule(50));
    }

    @Override
    public void askQuestion(String userQuestion)
    {
        getEngine().ifPresent(webEngine -> new SendMessageJob(dispatcher, webEngine, userQuestion, (String q) -> {
            webEngine.executeScript("window.chatApi.plain_message(`" + esqapeString(q) + "`)"); //$NON-NLS-1$ //$NON-NLS-2$
        }).schedule(50));
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

    private Optional<WebEngine> getEngine()
    {
        return dispatcher.dispatch(() -> {
            ensureWebViewCreated();
            return getWebView().map(view -> view.getEngine());
        }).orElse(Optional.empty());
    }

    private Optional<WebView> getWebView()
    {
        if (this.webView.isPresent())
        {
            return this.webView;
        }

        this.webView = dispatcher.dispatch(() -> {
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
                        webEngine.executeScript(CHAT_API_WINK);
                    }
                }

            });

            settingsProvider.getSettings().ifPresent(settings -> webEngine.load(settings.getChatURL().toString()));
            return view;
        });

        return this.webView;
    }

    private Optional<IViewPart> ensureWebViewCreated()
    {
        return ui.showView(ChatView.ID).map(view -> {
            view.setFocus();
            return view;
        });
    }

    private String esqapeString(String text)
    {
        return text;
    }

    public static class SendMessageJob
        extends Job
    {
        private final IDispatcher dispatcher;
        private final WebEngine webEngine;
        private final String message;
        private final Consumer<String> action;

        public SendMessageJob(IDispatcher dispatcher, WebEngine webEngine, String message, Consumer<String> action)
        {
            super("Send message"); //$NON-NLS-1$
            this.dispatcher = dispatcher;
            this.webEngine = webEngine;
            this.message = message;
            this.action = action;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            dispatcher.dispatch(() -> {
                if (webEngine.getTitle().contains(CHAT_PAGE_TITLE) || webEngine.getTitle().contains(WELCOME_PAGE_TITLE))
                {
                    action.accept(message);
                }
                else
                {
                    // reschedule
                    new SendMessageJob(dispatcher, webEngine, message, action).schedule(50);
                }
            });

            return Status.OK_STATUS;
        }
    }

    public static class IdeApiHandler
    {
        private ILog log;
        private IUI ui;

        public IdeApiHandler(ILog log, IUI ui)
        {
            this.log = log;
            this.ui = ui;
        }

        public void wink(String parameter)
        {
            System.out.println("Winked: " + parameter); //$NON-NLS-1$
        }

        public void paste_code(String code)
        {
            ui.getEditor().ifPresent(editor -> ui.getSelection().ifPresent(selection -> {
                try
                {
                    editor.getDocument().replace(selection.getOffset(), selection.getLength(), code);
                }
                catch (BadLocationException e)
                {
                    log.logError(e);
                }
            }));
        }
    }
}
