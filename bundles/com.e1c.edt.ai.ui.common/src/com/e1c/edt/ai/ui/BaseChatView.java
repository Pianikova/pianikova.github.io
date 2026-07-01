/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.util.LocalSelectionTransfer;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.DropTarget;
import org.eclipse.swt.dnd.DropTargetAdapter;
import org.eclipse.swt.dnd.DropTargetEvent;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.part.ViewPart;

import com.e1c.edt.ai.ui.handlers.IChatFileSelectionResolver;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import javafx.embed.swt.FXCanvas;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ScrollPane.ScrollBarPolicy;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.Background;
import javafx.scene.layout.Border;

/**
 * A class-holder view to dialog with the AI.
 *
 * @author Bogdan Sushkov
 */
public class BaseChatView
    extends ViewPart
{
    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "com.e1c.edt.ai.ui.views.ChatView"; //$NON-NLS-1$
    private FXCanvas canvas;
    @Inject
    IChatDialog chatDialog;
    @Inject
    IChat chat;
    @Inject
    IChatFileSelectionResolver selectionResolver;

    public BaseChatView()
    {
    }

    @Override
    public void createPartControl(Composite parent)
    {
        Preconditions.checkNotNull(parent);
        if (chatDialog == null)
        {
            BaseActivator.injectMembers(this);
        }

        Preconditions.checkNotNull(chatDialog, "chatDialog should be injected");

        parent.setLayout(new GridLayout());
        GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(parent);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);

        canvas = new FXCanvas(parent, SWT.BORDER);
        GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(canvas);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(canvas);

        var scrollPane = new ScrollPane();
        scrollPane.setHbarPolicy(ScrollBarPolicy.NEVER);
        scrollPane.setVbarPolicy(ScrollBarPolicy.NEVER);
        scrollPane.setBorder(Border.EMPTY);
        scrollPane.setBackground(Background.EMPTY);
        scrollPane.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);
        scrollPane.setPadding(Insets.EMPTY);

        AnchorPane anchorPane = new AnchorPane();
        anchorPane.setBackground(Background.EMPTY);
        anchorPane.setBorder(Border.EMPTY);
        anchorPane.setPadding(Insets.EMPTY);
        anchorPane.getChildren().add(scrollPane);
        AnchorPane.setTopAnchor(scrollPane, 0.0);
        AnchorPane.setBottomAnchor(scrollPane, 0.0);
        AnchorPane.setLeftAnchor(scrollPane, 0.0);
        AnchorPane.setRightAnchor(scrollPane, 0.0);

        Scene scene = new Scene(anchorPane);
        canvas.setScene(scene);
        chatDialog.show(scrollPane);

        // Added after the canvas so subclass footer controls (e.g. the EDT navigator drop zone)
        // are laid out below the chat.
        createFooterControls(parent);
    }

    /**
     * Hook for subclasses to add controls below the chat. Called after the chat canvas is created,
     * so anything added here is laid out at the bottom of the view. Default implementation adds
     * nothing.
     *
     * @param parent the view's root composite, laid out with a single-column {@link GridLayout}
     */
    protected void createFooterControls(Composite parent)
    {
        // no footer controls by default
    }

    /**
     * Creates a thin SWT strip that accepts objects dragged from the 1C:EDT Navigator. Such drags
     * use {@link LocalSelectionTransfer} (a JVM-memory-only transfer), which the JavaFX
     * {@code WebView} cannot see: {@link FXCanvas} hard-resets its drop target to its own fixed
     * transfer set on every {@code dragEnter}, so a navigator selection can never be accepted over
     * the canvas itself. A dedicated SWT control with its own {@link DropTarget} is therefore
     * required. OS-file drops keep working directly over the chat body (handled by JavaFX in
     * {@code Chat}). Intended to be called from {@link #createFooterControls(Composite)} by the
     * EDT-specific view only.
     */
    protected void createNavigatorDropZone(Composite parent)
    {
        if (chat == null || selectionResolver == null)
        {
            return;
        }

        var dropZone = new Label(parent, SWT.CENTER | SWT.BORDER);
        dropZone.setText(Messages.DropNavigatorObjectsHint);
        dropZone.setToolTipText(Messages.DropNavigatorObjectsHint);
        GridDataFactory.fillDefaults().grab(true, false).applyTo(dropZone);

        var localTransfer = LocalSelectionTransfer.getTransfer();
        var dropTarget = new DropTarget(dropZone, DND.DROP_COPY | DND.DROP_DEFAULT);
        dropTarget.setTransfer(new Transfer[] { localTransfer });
        dropTarget.addDropListener(new DropTargetAdapter()
        {
            @Override
            public void dragEnter(DropTargetEvent event)
            {
                accept(event);
            }

            @Override
            public void dragOver(DropTargetEvent event)
            {
                accept(event);
            }

            @Override
            public void dragOperationChanged(DropTargetEvent event)
            {
                accept(event);
            }

            @Override
            public void drop(DropTargetEvent event)
            {
                if (!localTransfer.isSupportedType(event.currentDataType))
                {
                    return;
                }

                var selection = localTransfer.getSelection();
                if (selection instanceof IStructuredSelection)
                {
                    var documents = selectionResolver.resolve(((IStructuredSelection)selection).toList());
                    if (!documents.isEmpty())
                    {
                        chat.addFiles(documents);
                    }
                }
            }

            private void accept(DropTargetEvent event)
            {
                if (!localTransfer.isSupportedType(event.currentDataType))
                {
                    return;
                }

                // Reject the drop (show the "no-drop" cursor) when the dragged selection has nothing
                // addable — e.g. group/collection nodes like "Documents" or "Catalogs".
                var selection = localTransfer.getSelection();
                var droppable = selection instanceof IStructuredSelection
                    && selectionResolver.canResolve(((IStructuredSelection)selection).toList());
                event.detail = droppable ? DND.DROP_COPY : DND.DROP_NONE;
            }
        });
    }

    @Override
    public void setFocus()
    {
        canvas.setFocus();
    }

    @Override
    public void dispose()
    {
        if (chatDialog != null)
        {
            chatDialog.hide();
        }

        super.dispose();
    }
}
