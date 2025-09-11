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
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class AIContextProvider
    implements IAIContextProvider
{
    private static final ProjectId DefaultProjectId = new ProjectId("", null); //$NON-NLS-1$
    private final IUI ui;
    private final IContentProvider contentProvider;
    private final IContextInitializer contextInitializer;
    private final IProjectIdProvider projectIdProvider;

    @Inject
    public AIContextProvider(IUI ui, IContentProvider contentProvider, IContextInitializer contextInitializer,
        IProjectIdProvider projectIdProvider)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(contentProvider);
        Preconditions.checkNotNull(contextInitializer);
        Preconditions.checkNotNull(projectIdProvider);
        this.ui = ui;
        this.contentProvider = contentProvider;
        this.contextInitializer = contextInitializer;
        this.projectIdProvider = projectIdProvider;
    }

    @Override
    public Optional<AIContext> create(SourceViewer sourceViewer, AITarget target, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(sourceViewer);
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(cancellationToken);
        var file = ui.getFile(sourceViewer);
        String path = ""; //$NON-NLS-1$
        ProjectId projectId = DefaultProjectId;
        if (file.isPresent())
        {
            path = file.get().getFullPath().makeRelative().toPortableString();
            projectId = projectIdProvider.getProjectId(path, cancellationToken).orElse(DefaultProjectId);
        }

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
                new AIContext(projectId, textWidget.getCaretOffset(), content.text,
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
            aiContext = new AIContext(projectId, textWidget.getCaretOffset(),
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

        return contextInitializer.initialize(aiContext);
    }
}