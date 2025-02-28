/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.AIContextKind;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IContextInitializer;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.assistent.model.ProjectId;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class AIContextProvider
    implements IAIContextProvider
{
    private static final ProjectId DefaultProjectId = new ProjectId(""); //$NON-NLS-1$
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
    public Optional<AIContext> create(AITarget target, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(cancellationToken);
        var textWidget = target.getTextWidget();
        return ui.getSourceViewer(textWidget)
            .flatMap(sourceViewer -> create(textWidget, sourceViewer, target, cancellationToken));
    }

    private Optional<AIContext> create(StyledText textWidget, SourceViewer sourceViewer, AITarget target,
        ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(sourceViewer);
        Preconditions.checkNotNull(target);
        Preconditions.checkNotNull(cancellationToken);
        var file = ui.getEditor(sourceViewer).map(editor -> editor.getEditorInput().getAdapter(IFile.class));
        String path = ""; //$NON-NLS-1$
        ProjectId projectId = DefaultProjectId;
        if (!file.isEmpty())
        {
            path = file.get().getFullPath().makeRelative().toPortableString();
            projectId = projectIdProvider.getProjectId(path, cancellationToken).orElse(DefaultProjectId);
        }

        var content = contentProvider.get(textWidget, textWidget.getCaretOffset());
        AIContext aiContext;
        if (target.isPreferSelection() && !content.selectionText.isBlank())
        {
            aiContext =
                new AIContext(projectId, AIContextKind.ActiveEditor, textWidget.getCaretOffset(), content.text,
                    content.offset, path,
                content.selectionText, content.selectionOffset);
        }
        else
        {
            aiContext = new AIContext(projectId, AIContextKind.ActiveEditor, textWidget.getCaretOffset(),
                content.text,
                content.offset, path,
                content.text,
                content.offset);
        }

        return contextInitializer.initialize(aiContext);
    }
}