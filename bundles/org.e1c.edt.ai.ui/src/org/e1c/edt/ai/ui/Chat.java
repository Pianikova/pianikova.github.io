/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.function.Consumer;

import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsProvider;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.xtext.ui.editor.XtextEditor;

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
    private final IdeApiHandler handler;
    private WebView webView;

    public Chat(ILog log, ISettingsProvider settingsProvider)
    {
        this.settingsProvider = settingsProvider;
        handler = new IdeApiHandler(log);
    }

    @Override
    public void reviewCode(String codeSnippet)
    {
        WebEngine webEngine = getEngine();
        new SendMessageJob(webEngine, codeSnippet, (String code) -> {
            webEngine.executeScript("window.chatApi.review_code(`" + esqapeString(code) + "`)"); //$NON-NLS-1$ //$NON-NLS-2$
        }).schedule(50);
    }

    @Override
    public void explainCode(String codeSnippet)
    {
        WebEngine webEngine = getEngine();
        new SendMessageJob(webEngine, codeSnippet, (String code) -> {
            webEngine.executeScript("window.chatApi.comment_code(`" + esqapeString(code) + "`)"); //$NON-NLS-1$ //$NON-NLS-2$
        }).schedule(50);
    }

    @Override
    public void fixCode(String codeSnippet)
    {
        WebEngine webEngine = getEngine();
        new SendMessageJob(webEngine, codeSnippet, (String code) -> {
            webEngine.executeScript("window.chatApi.fix_code(`" + esqapeString(code) + "`)"); //$NON-NLS-1$ //$NON-NLS-2$
        }).schedule(50);
    }

    @Override
    public void generateDocComments(String method)
    {
        WebEngine webEngine = getEngine();
        new SendMessageJob(webEngine, method, (String code) -> {
            webEngine.executeScript("window.chatApi.document_code(`" + esqapeString(code) + "`)"); //$NON-NLS-1$ //$NON-NLS-2$
        }).schedule(50);
    }

    @Override
    public void askQuestion(String userQuestion)
    {
        WebEngine webEngine = getEngine();
        new SendMessageJob(webEngine, userQuestion, (String q) -> {
            webEngine.executeScript("window.chatApi.plain_message(`" + esqapeString(q) + "`)"); //$NON-NLS-1$ //$NON-NLS-2$
        }).schedule(50);
    }

    @Override
    public void show(ScrollPane pane)
    {
        pane.setContent(getWebView());
        webView.setFocusTraversable(true);

        pane.widthProperty().addListener(new ChangeListener<Object>()
        {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue)
            {
                Double width = (Double)newValue;
                getWebView().setPrefWidth(width);
            }
        });
        pane.heightProperty().addListener(new ChangeListener<Object>()
        {
            @Override
            public void changed(ObservableValue<?> observable, Object oldValue, Object newValue)
            {
                Double height = (Double)newValue;
                getWebView().setPrefHeight(height);
            }
        });
    }

    private WebEngine getEngine()
    {
        return getWebView().getEngine();
    }

    private WebView getWebView()
    {
        if (webView != null)
        {
            return webView;
        }

        Display.getDefault().syncExec(() -> {
            webView = new WebView();
            webView.setLayoutX(-1);
            webView.setLayoutY(-1);
            WebEngine webEngine = webView.getEngine();
            webEngine.titleProperty().addListener(new ChangeListener<String>()
            {
                @Override
                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue)
                {
                    if (newValue.contains(WELCOME_PAGE_TITLE))
                    {
                        // initialize only once, no need this listener anymore
                        webEngine.titleProperty().removeListener(this);

                        JSObject window = (JSObject)getEngine().executeScript("window"); //$NON-NLS-1$
                        window.setMember(IDE_API, handler);
                        webEngine.executeScript(CHAT_API_WINK);
                    }
                }

            });

            webEngine.load(settingsProvider.getSettings().getChatURL().toString());
        });

        return webView;
    }

    private String esqapeString(String text)
    {
        return text;
    }

    public static class SendMessageJob
        extends Job
    {
        private WebEngine webEngine;
        private String message;
        private Consumer<String> action;

        public SendMessageJob(WebEngine webEngine, String message, Consumer<String> action)
        {
            super("Send message"); //$NON-NLS-1$
            this.webEngine = webEngine;
            this.message = message;
            this.action = action;
        }

        @Override
        protected IStatus run(IProgressMonitor monitor)
        {
            Display.getDefault().asyncExec(new Runnable()
            {
                @Override
                public void run()
                {
                    if (webEngine.getTitle().contains(CHAT_PAGE_TITLE)
                        || webEngine.getTitle().contains(WELCOME_PAGE_TITLE))
                    {
                        action.accept(message);

                    }
                    else
                    {
                        // reschedule
                        new SendMessageJob(webEngine, message, action).schedule(50);
                    }
                }
            });
            return Status.OK_STATUS;
        }
    }

    public static class IdeApiHandler
    {
        private ILog log;

        public IdeApiHandler(ILog log)
        {
            this.log = log;
        }

        public void wink(String parameter)
        {
            System.out.println("Winked: " + parameter); //$NON-NLS-1$
        }

        public void paste_code(String code)
        {
            try
            {
                IWorkbenchPage activePage = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

                if (activePage != null)
                {
                    IEditorPart editor = activePage.getActiveEditor();

                    if (editor != null)
                    {
                        XtextEditor xtextEditor = editor.getAdapter(XtextEditor.class);

                        try
                        {
                            ITextSelection textSelection =
                                (ITextSelection)xtextEditor.getSelectionProvider().getSelection();
                            xtextEditor.getDocument()
                                .replace(textSelection.getOffset(), textSelection.getLength(), code);
                        }
                        catch (BadLocationException e)
                        {
                            log.logError(e);
                        }
                    }
                }
            }
            catch (Exception e)
            {
                Activator.logError(e);
            }
        }
    }
}
