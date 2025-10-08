/**
 *
 */
package com.e1c.edt.ai.ui;

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
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.IVisualContextProvider;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ContextMenuInterceptor implements IContextMenuInterceptor
{
    private final IDispatcher dispatcher;
    private final IVisualContextProvider visualContextProviewr;
    private final ITextActions textActions;
    private final IUI ui;

    @Inject
    public ContextMenuInterceptor(IDispatcher dispatcher, IVisualContextProvider visualContextProviewr,
        ITextActions textActions, IUI ui)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(visualContextProviewr);
        Preconditions.checkNotNull(textActions);
        Preconditions.checkNotNull(ui);
        this.dispatcher = dispatcher;
        this.visualContextProviewr = visualContextProviewr;
        this.textActions = textActions;
        this.ui = ui;
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
            private boolean isFirstClick = true;

            @Override
            public void mouseDoubleClick(MouseEvent e)
            {
                if (e.button == 2)
                {
                    isFirstClick = false;
                    handle();
                }
            }

            @Override
            public void mouseDown(MouseEvent e)
            {
                if (isFirstClick && e.button == 2)
                {
                    isFirstClick = false;
                    handle();
                }
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
    }

    private MenuItem createMenuItem(Menu menu, IText text, TextAction textAction, boolean allowForEmptyText)
    {
        var menuItem = new MenuItem(menu, SWT.PUSH);
        menuItem.setData(new MenuData(text));
        menuItem.setText(textAction.title);
        menuItem.setImage(BaseActivator.getImage(textAction.imageName));
        var textListener = new TextListener(text, menuItem, allowForEmptyText);
        text.getControl().addFocusListener(textListener);
        text.addModifyListener(textListener);
        menuItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> executeAction(text, textAction, textListener)));
        return menuItem;
    }

    private void executeAction(IText text, TextAction textAction, TextListener textListener)
    {
        var cancellationTokenSource = new CancellationTokenSource();
        textListener.cancellationTokenSource = cancellationTokenSource;
        var context = visualContextProviewr.create(text.getControl(), cancellationTokenSource);
        var improvementsSource = textActions.ceateTextImprovementsSource(context, textAction, cancellationTokenSource);
        improvementsSource.subscribe(new IObserver<TextImprovements>()
        {
            @Override
            public void onNext(TextImprovements textImprovements)
            {
                dispatcher.dispatch(() -> {
                    textListener.isSuppresed = true;
                    try
                    {
                        text.setContent(textImprovements.getText());
                    }
                    finally
                    {
                        textListener.isSuppresed = false;
                    }
                });
            }

            @Override
            public void onError(Throwable error)
            {
                //
            }

            @Override
            public void onCompleted()
            {
                dispatcher.dispatch(() -> text.selectAll());
            }
        });
    }

    private interface IText
    {
        Control getControl();

        String getContent();

        void setContent(String content);

        void selectAll();

        void addModifyListener(ModifyListener modifyListener);
    }

    private final class TextListener
        implements FocusListener, ModifyListener
    {
        private final IText text;
        private final MenuItem menuItem;
        private final boolean allowForEmptyText;
        public CancellationTokenSource cancellationTokenSource;
        public boolean isSuppresed;

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
            //
        }

        @Override
        public void focusLost(FocusEvent e)
        {
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
            if (menuItem != null)
            {
                menuItem.setEnabled(allowForEmptyText || !text.getContent().trim().isBlank());
            }
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
        public MenuData(IText text)
        {
            //
        }
    }
}