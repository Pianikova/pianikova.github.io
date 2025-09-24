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
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;

import com.e1c.edt.ai.CancellationTokenSource;
import com.e1c.edt.ai.IObserver;
import com.e1c.edt.ai.IVisualContextProvider;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ContextMenuInterceptor implements IContextMenuInterceptor
{
    private final IDispatcher dispatcher;
    private final IVisualContextProvider visualContextProviewr;
    private final ITextActions textActions;

    @Inject
    public ContextMenuInterceptor(IDispatcher dispatcher, IVisualContextProvider visualContextProviewr,
        ITextActions textActions)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(visualContextProviewr);
        Preconditions.checkNotNull(textActions);
        this.dispatcher = dispatcher;
        this.visualContextProviewr = visualContextProviewr;
        this.textActions = textActions;
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
            var styledText = (StyledText)event.widget;
            if (isEditable(styledText))
            {
                initializeMenu(styledText);
            }
        }
    }

    private boolean isEditable(StyledText text)
    {
        return !text.isDisposed() && text.isVisible() && text.isEnabled() && text.getEditable();
    }

    private void initializeMenu(StyledText text)
    {
        var menu = text.getMenu();
        if (menu == null)
        {
            menu = new Menu(text);
            text.setMenu(menu);
        }
        else
        {
            if (!isContextMenu(menu) || hasMenuItems(menu))
            {
                return;
            }
        }

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

    private void addMenuItems(Menu menu, StyledText text)
    {
        if (menu.getItemCount() > 0)
        {
            // Add a separator
            new MenuItem(menu, SWT.SEPARATOR);
        }

        // Add menu items
        createMenuItem(menu, text, TextAction.SUGGEST_YOU_OPTION, true);
        createMenuItem(menu, text, TextAction.CORRECT_ERRORS, false);
        createMenuItem(menu, text, TextAction.IN_OTHER_WORDS, false);
        createMenuItem(menu, text, TextAction.IMPROVE_STYLE, false);
    }

    private MenuItem createMenuItem(Menu menu, StyledText text, TextAction textAction, boolean allowForEmptyText)
    {
        var menuItem = new MenuItem(menu, SWT.PUSH);
        menuItem.setData(new MenuData(text));
        menuItem.setText(textAction.title);
        menuItem.setImage(BaseActivator.getImage(textAction.imageName));
        var styledTextListener = new StyledTextListener(text, menuItem, allowForEmptyText);
        text.addFocusListener(styledTextListener);
        text.addModifyListener(styledTextListener);
        menuItem.addSelectionListener(SelectionListener.widgetSelectedAdapter(e -> {
            var cancellationTokenSource = new CancellationTokenSource();
            styledTextListener.cancellationTokenSource = cancellationTokenSource;
            var context = visualContextProviewr.create(text, cancellationTokenSource);
            var improvementsSource =
                textActions.ceateTextImprovementsSource(context, textAction, cancellationTokenSource);
            improvementsSource.subscribe(new IObserver<TextImprovements>()
            {
                @Override
                public void onNext(TextImprovements textImprovements)
                {
                    dispatcher.dispatch(() -> {
                        styledTextListener.isSuppresed = true;
                        try
                        {
                            text.setText(textImprovements.getText());
                        }
                        finally
                        {
                            styledTextListener.isSuppresed = false;
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
                    //
                }
            });
        }));

        return menuItem;
    }

    private final class StyledTextListener
        implements FocusListener, ModifyListener
    {
        private final StyledText text;
        private final MenuItem menuItem;
        private final boolean allowForEmptyText;
        public CancellationTokenSource cancellationTokenSource;
        public boolean isSuppresed;

        public StyledTextListener(StyledText text, MenuItem menuItem, boolean allowForEmptyText)
        {
            this.text = text;
            this.menuItem = menuItem;
            this.allowForEmptyText = allowForEmptyText;
            SetIsEnabled();
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
            SetIsEnabled();
        }

        private void SetIsEnabled()
        {
            menuItem.setEnabled(allowForEmptyText || !text.getText().trim().isBlank());
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
        public MenuData(StyledText text)
        {
            //
        }
    }
}