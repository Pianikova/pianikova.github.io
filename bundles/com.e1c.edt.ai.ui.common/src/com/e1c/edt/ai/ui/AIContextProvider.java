/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.lang.ref.WeakReference;
import java.util.Optional;

import org.eclipse.jface.text.source.SourceViewer;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IContextInitializer;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class AIContextProvider
    implements IAIContextProvider
{
    private final IUI ui;
    private final IContentProvider contentProvider;
    private final IContextInitializer contextInitializer;

    @Inject
    public AIContextProvider(IUI ui, IContentProvider contentProvider, IContextInitializer contextInitializer)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(contentProvider);
        Preconditions.checkNotNull(contextInitializer);
        this.ui = ui;
        this.contentProvider = contentProvider;
        this.contextInitializer = contextInitializer;
    }

    @Override
    public Optional<AIContext> create(SourceViewer sourceViewer, AITarget target, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(sourceViewer);
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(cancellationToken);
        var file = ui.getFile(sourceViewer);
        String path = ""; //$NON-NLS-1$
        if (file.isEmpty())
        {
            return Optional.empty();
        }

        var workspaceFile = file.get();
        var project = workspaceFile.getProject();
        if (!project.isAccessible())
        {
            return Optional.empty();
        }

        path = workspaceFile.getFullPath().makeRelative().toPortableString();

        var textWidget = sourceViewer.getTextWidget();
        if (textWidget == null)
        {
            return Optional.empty();
        }

        var content = contentProvider.get(textWidget, textWidget.getCaretOffset());
        AIContext aiContext;
        if (target.isPreferSelection() && !content.selectionText.isBlank())
        {
            aiContext =
                new AIContext(project, textWidget.getCaretOffset(), content.text,
                    content.offset, path,
                    content.selectionText, content.selectionOffset, sourceViewer.getDocument(),
                    () -> Optional.ofNullable(new WeakReference<>(sourceViewer))
                        .map(i -> i.get())
                        .map(i -> i.getTextWidget())
                        .map(i -> i.isDisposed())
                        .orElse(true));
        }
        else
        {
            aiContext = new AIContext(project, textWidget.getCaretOffset(),
                content.text,
                content.offset, path,
                content.text,
                content.offset, sourceViewer.getDocument(),
                () -> Optional.ofNullable(new WeakReference<>(sourceViewer))
                    .map(i -> i.get())
                    .map(i -> i.getTextWidget())
                    .map(i -> i.isDisposed())
                    .orElse(true));
        }

        return contextInitializer.initialize(aiContext, target.getLimitSize());
    }
}
