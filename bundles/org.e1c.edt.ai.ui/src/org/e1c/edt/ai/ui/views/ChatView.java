/*
 * Copyright (C) 2023, 1C
 */
package org.e1c.edt.ai.ui.views;

import org.e1c.edt.ai.ui.Composition;
import org.e1c.edt.ai.ui.IChatDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.part.ViewPart;

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
public class ChatView
    extends ViewPart
{
    /**
     * The ID of the view as specified by the extension.
     */
    public static final String ID = "org.e1c.edt.ai.ui.views.ChatView"; //$NON-NLS-1$
    private FXCanvas canvas;
    private final IChatDialog chatDialog;

    public ChatView()
    {
        chatDialog = Composition.getChatDialog();
    }

    @Override
    public void createPartControl(Composite parent)
    {
        parent.setLayout(new GridLayout());
        GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(parent);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(parent);

        canvas = new FXCanvas(parent, SWT.BORDER);
        GridLayoutFactory.fillDefaults().spacing(0, 0).applyTo(canvas);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(canvas);

        AnchorPane pane = new AnchorPane();
        ScrollPane sp = new ScrollPane();

        sp.setHbarPolicy(ScrollBarPolicy.NEVER);
        sp.setVbarPolicy(ScrollBarPolicy.NEVER);
        sp.setBorder(Border.EMPTY);

        // Workaroud to avoid pane's border / background decoration
//        sp.setLayoutX(-1);
//        sp.setLayoutY(-1);

        sp.setBackground(Background.EMPTY);

        pane.setBackground(Background.EMPTY);
        pane.setBorder(Border.EMPTY);
        pane.setPadding(Insets.EMPTY);
        pane.getChildren().add(sp);
        AnchorPane.setTopAnchor(sp, 0.0);
        AnchorPane.setBottomAnchor(sp, 0.0);
        AnchorPane.setLeftAnchor(sp, 0.0);
        AnchorPane.setRightAnchor(sp, 0.0);
        sp.setMaxSize(Double.MAX_VALUE, Double.MAX_VALUE);

        sp.setPadding(Insets.EMPTY);

        Scene scene = new Scene(pane);
        canvas.setScene(scene);

        chatDialog.show(sp);
    }

    @Override
    public void setFocus()
    {
        canvas.setFocus();
    }

}
