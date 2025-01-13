/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.AIContextKind;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IContextInitializer;
import org.e1c.edt.ai.IProjectIdProvider;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.swt.custom.StyledText;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class AIContextProvider
    implements IAIContextProvider
{
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
        if (file.isEmpty())
        {
            return Optional.empty();
        }

        var path = file.get().getFullPath().makeRelative().toPortableString();
        var content = contentProvider.get(textWidget);
        AIContext aiContext;
        var optionalProjectId = projectIdProvider.getProjectId(path, cancellationToken);
        if (optionalProjectId.isEmpty())
        {
            return Optional.empty();
        }

        if (target.isPreferSelection() && !content.selectionText.isBlank())
        {
            aiContext =
                new AIContext(optionalProjectId.get(), AIContextKind.ActiveEditor, textWidget.getCaretOffset(), content.text,
                    content.offset, path,
                content.selectionText, content.selectionOffset);
        }
        else
        {
            aiContext = new AIContext(optionalProjectId.get(), AIContextKind.ActiveEditor, textWidget.getCaretOffset(),
                content.text,
                content.offset, path,
                content.text,
                content.offset);
        }

        return contextInitializer.initialize(aiContext);
    }
}