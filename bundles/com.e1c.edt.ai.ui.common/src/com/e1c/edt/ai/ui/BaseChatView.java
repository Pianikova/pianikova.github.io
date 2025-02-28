/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

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

    public BaseChatView()
    {
        BaseActivator.injectMembers(this);
    }

    @Override
    public void createPartControl(Composite parent)
    {
        Preconditions.checkNotNull(parent);
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
    }

    @Override
    public void setFocus()
    {
        canvas.setFocus();
    }
}
