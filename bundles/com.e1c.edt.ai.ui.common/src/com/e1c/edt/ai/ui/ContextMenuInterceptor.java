/**
 *
 */
package com.e1c.edt.ai.ui;

import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Text;

import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IConversationFacade;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IVisualContextProvider;
import com.e1c.edt.ai.assistent.SendMessageResult;
import com.e1c.edt.ai.assistent.SendUserMessageRequest;
import com.e1c.edt.ai.assistent.model.SkillExecutionRequest;
import com.e1c.edt.ai.assistent.model.VisualContext;
import com.e1c.edt.ai.assistent.model.VisualField;
import com.e1c.edt.ai.skills.ISkillExecutor;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ContextMenuInterceptor
    implements IInitializable
{
    private final IDispatcher dispatcher;
    private final IVisualContextProvider visualContextProviewr;
    private final ISkillExecutor skillExecutor;
    private final IConversationFacade conversationFacade;
    private final ILog log;
    private final IUI ui;
    private final ISettings settings;
    private final ICurrentProjectResolver currentProjectResolver;
    private CancellationTokenSource currentCancellationTokenSource;

    @Inject
    public ContextMenuInterceptor(IDispatcher dispatcher, IVisualContextProvider visualContextProviewr,
        ISkillExecutor skillExecutor, IConversationFacade conversationFacade, ILog log, IUI ui, ISettings settings,
        ICurrentProjectResolver currentProjectResolver)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(visualContextProviewr);
        Preconditions.checkNotNull(skillExecutor);
        Preconditions.checkNotNull(conversationFacade);
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(currentProjectResolver);
        this.dispatcher = dispatcher;
        this.visualContextProviewr = visualContextProviewr;
        this.skillExecutor = skillExecutor;
        this.conversationFacade = conversationFacade;
        this.log = log;
        this.ui = ui;
        this.settings = settings;
        this.currentProjectResolver = currentProjectResolver;
    }

    @Override
    public void initialize()
    {
        dispatcher.dispatchAsync(() ->
        Display.getDefault().addFilter(SWT.FOCUSED, event -> handleOnFocusEvent(event)));
    }

    public void handleOnFocusEvent(Event event)
    {
        if (event.widget instanceof StyledText)
        {
            var text = (StyledText)event.widget;
            if (isEnabled(text) && text.getEditable())
            {
                if (ui.getSourceViewer(text).isPresent())
                {
                    return;
                }

                initialize(new IText()
                {
                    @Override
                    public Control getControl()
                    {
                        return text;
                    }

                    @Override
                    public String getContent()
                    {
                        return text.getText();
                    }

                    @Override
                    public String getSelectedText()
                    {
                        return text.getSelectionText();
                    }

                    @Override
                    public void setContent(String content)
                    {
                        text.setText(content);
                    }

                    @Override
                    public void selectAll()
                    {
                        text.selectAll();
                    }

                    @Override
                    public void addModifyListener(ModifyListener modifyListener)
                    {
                        text.addModifyListener(modifyListener);
                    }

                    @Override
                    public void removeModifyListener(ModifyListener modifyListener)
                    {
                        text.removeModifyListener(modifyListener);
                    }
                });
            }

            return;
        }

        if (event.widget instanceof Text)
        {
            var text = (Text)event.widget;
            if (isEnabled(text) && text.getEditable())
            {
                initialize(new IText()
                {
                    @Override
                    public Control getControl()
                    {
                        return text;
                    }

                    @Override
                    public String getContent()
                    {
                        return text.getText();
                    }

                    @Override
                    public String getSelectedText()
                    {
                        return text.getSelectionText();
                    }

                    @Override
                    public void setContent(String content)
                    {
                        text.setText(content);
                    }

                    @Override
                    public void selectAll()
                    {
                        text.selectAll();
                    }

                    @Override
                    public void addModifyListener(ModifyListener modifyListener)
                    {
                        text.addModifyListener(modifyListener);
                    }

                    @Override
                    public void removeModifyListener(ModifyListener modifyListener)
                    {
                        text.removeModifyListener(modifyListener);
                    }
                });
            }

            return;
        }
    }

    private boolean isEnabled(Control control)
    {
        return !control.isDisposed() && control.isVisible() && control.isEnabled();
    }

    private void initialize(IText text)
    {
        var menu = text.getControl().getMenu();
        if (menu == null)
        {
            menu = new Menu(text.getControl());
            text.getControl().setMenu(menu);
        }
        else
        {
            if (!isContextMenu(menu) || hasMenuItems(menu))
            {
                return;
            }
        }

        var context = visualContextProviewr.create(text.getControl(), CancellationTokens.NONE);
        if (context.isEmpty())
        {
            return;
        }

        text.getControl().addMouseListener(new MouseListener()
        {
            @Override
            public void mouseDoubleClick(MouseEvent e)
            {
                if (e.button == 2)
                {
                    handle();
                }
            }

            @Override
            public void mouseDown(MouseEvent e)
            {
                //
            }

            @Override
            public void mouseUp(MouseEvent e)
            {
                //
            }

            private void handle()
            {
                var styledTextListener = new TextListener(text, null, true);
                text.getControl().addFocusListener(styledTextListener);
                text.addModifyListener(styledTextListener);
                var action = text.getContent().isBlank() ? TextAction.SUGGEST_YOUR_OPTION : TextAction.CORRECT_ERRORS;
                executeAction(text, action, styledTextListener);
            }
        });

        addMenuItems(menu, text);
    }

    private boolean isContextMenu(Menu menu)
    {
        return (menu.getStyle() & SWT.POP_UP) != 0;
    }

    private boolean hasMenuItems(Menu menu)
    {
        for (var item : menu.getItems())
        {
            if (item.getData() instanceof MenuData)
            {
                return true;
            }
        }

        return false;
    }

    private void addMenuItems(Menu menu, IText text)
    {
        if (menu.getItemCount() > 0)
        {
            // Add a separator
            new MenuItem(menu, SWT.SEPARATOR);
        }

        // Add menu items
        createMenuItem(menu, text, TextAction.SUGGEST_YOUR_OPTION, true);
        createMenuItem(menu, text, TextAction.CORRECT_ERRORS, false);
        createMenuItem(menu, text, TextAction.IN_OTHER_WORDS, false);
        createMenuItem(menu, text, TextAction.IMPROVE_STYLE, false);
        menu.addListener(SWT.Show, event -> refreshMenuItems(menu));
    }

    private void refreshMenuItems(Menu menu)
    {
        var projectAvailable = currentProjectResolver.resolveOrDefault().isPresent();
        for (var item : menu.getItems())
        {
            var data = item.getData();
            if (data instanceof MenuData)
            {
                ((MenuData)data).textListener.setProjectAvailable(projectAvailable);
            }
        }
    }

    private MenuItem createMenuItem(Menu menu, IText text, TextAction textAction, boolean allowForEmptyText)
    {
        var menuItem = new MenuItem(menu, SWT.PUSH);
        menuItem.setText(textAction.title);
        menuItem.setImage(BaseActivator.getImage(textAction.imageName));
        var textListener = new TextListener(text, menuItem, allowForEmptyText);
        menuItem.setData(new MenuData(textListener));
        text.getControl().addFocusListener(textListener);
        text.addModifyListener(textListener);
        menuItem.addDisposeListener(e -> removeTextListener(text, textListener));
        menuItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            textListener.ignoreNextFocusLost();
            executeAction(text, textAction, textListener);
        }));
        return menuItem;
    }

    private void removeTextListener(IText text, TextListener textListener)
    {
        if (!text.getControl().isDisposed())
        {
            text.getControl().removeFocusListener(textListener);
            text.removeModifyListener(textListener);
        }
    }

    @SuppressWarnings("nls")
    private void executeAction(IText text, TextAction textAction, TextListener textListener)
    {
        if (!settings.isEnabled())
        {
            return;
        }

        var cancellationTokenSource = new CancellationTokenSource();
        // Only one text action runs at a time, a new one cancels the previous
        synchronized (this)
        {
            if (currentCancellationTokenSource != null)
            {
                currentCancellationTokenSource.cancel();
            }

            currentCancellationTokenSource = cancellationTokenSource;
        }

        textListener.cancellationTokenSource = cancellationTokenSource;

        // Skill parameters are captured on the UI thread, before the background job starts
        var context = visualContextProviewr.create(text.getControl(), cancellationTokenSource);
        var project = currentProjectResolver.resolveOrDefault();
        if (project.isEmpty())
        {
            log.warning("AI Context Menu", () -> "Cannot determine project for text action"); //$NON-NLS-1$ //$NON-NLS-2$
            return;
        }
        var isMultiline = (text.getControl().getStyle() & SWT.MULTI) != 0;
        // @formatter:off
        var parameters = Map.of(
            "field_name", sanitize(findFocusedFieldName(context)),
            "field_value", sanitize(text.getContent()),
            "selected_text", sanitize(text.getSelectedText()),
            "is_multiline", String.valueOf(isMultiline),
            "language", settings.getLanguage());
        // @formatter:on

        var job = dispatcher.createJob(NLS.bind(Messages.TextActionJobName, textAction.title), jobCtx -> {
            var request = new SkillExecutionRequest(textAction.skillId, parameters);
            var operation = skillExecutor.executeAsync(request, cancellationTokenSource)
                .thenCompose(result -> conversationFacade.sendAsync(
                    new SendUserMessageRequest(project.get(), result.getPrompt(), null, true,
                        null, null, null, result.getAllowedTools().orElse(null),
                        result.getCompletionPolicy().orElse(null)),
                    cancellationTokenSource))
                .thenAccept(resultMessage -> applyResult(text, textListener, cancellationTokenSource, resultMessage))
                .exceptionally(error -> {
                    log.logError(error);
                    return null;
                });
            // Hold the job open for the whole request, otherwise the progress UI disappears at once.
            JobFutures.await(jobCtx, operation, cancellationTokenSource);
        }, false, cancellationTokenSource);
        // A text action is triggered by the user and is short — it must not queue behind long jobs.
        job.setPriority(Job.INTERACTIVE);
        job.schedule();
    }

    private void applyResult(IText text, TextListener textListener, ICancellationToken cancellationToken,
        SendMessageResult resultMessage)
    {
        if (resultMessage == null || cancellationToken.isCanceled())
        {
            return;
        }

        var newText = resultMessage.getText();
        if (newText == null || newText.isBlank())
        {
            return;
        }

        dispatcher.dispatch(() -> {
            if (cancellationToken.isCanceled() || text.getControl().isDisposed())
            {
                return;
            }

            textListener.isSuppresed = true;
            try
            {
                text.setContent(newText.trim());
            }
            finally
            {
                textListener.isSuppresed = false;
            }

            text.selectAll();
        });
    }

    private String findFocusedFieldName(VisualContext context)
    {
        var name = findFocusedFieldName(context.fields);
        if (name != null)
        {
            return name;
        }

        if (context.groups != null)
        {
            for (var group : context.groups)
            {
                name = findFocusedFieldName(group.fields);
                if (name != null)
                {
                    return name;
                }
            }
        }

        return ""; //$NON-NLS-1$
    }

    private String findFocusedFieldName(List<VisualField> fields)
    {
        if (fields == null)
        {
            return null;
        }

        for (var field : fields)
        {
            if (Boolean.TRUE.equals(field.isFocused) && field.name != null)
            {
                return field.name;
            }
        }

        return null;
    }

    @SuppressWarnings("nls")
    private String sanitize(String value)
    {
        if (value == null)
        {
            return "";
        }

        // A literal tool directive in a field value would be executed by the skill template
        // renderer after placeholder substitution - break it apart
        return value.replace("!tool(", "!tool (");
    }

    private interface IText
    {
        Control getControl();

        String getContent();

        String getSelectedText();

        void setContent(String content);

        void selectAll();

        void addModifyListener(ModifyListener modifyListener);

        void removeModifyListener(ModifyListener modifyListener);
    }

    private final class TextListener
        implements FocusListener, ModifyListener
    {
        private final IText text;
        private final MenuItem menuItem;
        private final boolean allowForEmptyText;
        private boolean projectAvailable;
        public CancellationTokenSource cancellationTokenSource;
        public boolean isSuppresed;
        private boolean ignoreNextFocusLost;

        public TextListener(IText text, MenuItem menuItem, boolean allowForEmptyText)
        {
            this.text = text;
            this.menuItem = menuItem;
            this.allowForEmptyText = allowForEmptyText;
            setIsEnabled();
        }

        @Override
        public void focusGained(FocusEvent e)
        {
            ignoreNextFocusLost = false;
        }

        @Override
        public void focusLost(FocusEvent e)
        {
            if (ignoreNextFocusLost)
            {
                ignoreNextFocusLost = false;
                return;
            }
            cancel();
        }

        @Override
        public void modifyText(ModifyEvent e)
        {
            cancel();
            setIsEnabled();
        }

        private void setIsEnabled()
        {
            if (menuItem != null && !menuItem.isDisposed() && !text.getControl().isDisposed())
            {
                menuItem.setEnabled(projectAvailable && settings.isEnabled()
                    && (allowForEmptyText || !text.getContent().trim().isBlank()));
            }
        }

        private void setProjectAvailable(boolean value)
        {
            projectAvailable = value;
            setIsEnabled();
        }

        private void ignoreNextFocusLost()
        {
            ignoreNextFocusLost = true;
        }

        private void cancel()
        {
            var current = cancellationTokenSource;
            if (current != null && !isSuppresed)
            {
                cancellationTokenSource.cancel();
            }
        }
    }

    private class MenuData
    {
        private final TextListener textListener;

        public MenuData(TextListener textListener)
        {
            this.textListener = Preconditions.checkNotNull(textListener);
        }
    }
}
