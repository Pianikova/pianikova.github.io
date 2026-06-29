/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.text.MessageFormat;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareUI;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.TracingSources;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * Default {@link IDiffPreviewOpener}: opens a standard read-only Eclipse compare editor (current vs
 * proposed content) using the in-memory snapshot stored at RENDER time.
 */
public class DiffPreviewOpener
    implements IDiffPreviewOpener
{
    private static final String AI_CHAT = "AI Chat"; //$NON-NLS-1$

    private final IDiffPreviewStore store;
    private final ILog log;

    @Inject
    public DiffPreviewOpener(IDiffPreviewStore store, ILog log)
    {
        Preconditions.checkNotNull(store);
        Preconditions.checkNotNull(log);
        this.store = store;
        this.log = log;
    }

    @Override
    public void openDiff(String token)
    {
        var optionalPreview = store.get(token);
        if (optionalPreview.isEmpty())
        {
            log.trace(TracingSources.CHAT, AI_CHAT,
                () -> "No diff preview registered for token: " + token); //$NON-NLS-1$
            return;
        }

        var preview = optionalPreview.get();

        // The element name is the human-friendly breadcrumb; the type (for syntax coloring) is
        // derived from the real file name's extension.
        var realFileName = new java.io.File(preview.getFilePath()).getName();
        var left =
            new StringTypedElement(preview.getDisplayName(), realFileName, preview.getOriginalContent());
        var right =
            new StringTypedElement(preview.getDisplayName(), realFileName, preview.getProposedContent());

        var configuration = new CompareConfiguration();
        configuration.setLeftEditable(false);
        configuration.setRightEditable(false);
        configuration.setLeftLabel(MessageFormat.format(Messages.DiffCompareCurrentLabel, preview.getFilePath()));
        configuration.setRightLabel(Messages.DiffCompareProposedLabel);

        var input = new DiffCompareEditorInput(left, right, configuration);
        input.setTitle(preview.getDisplayName());

        CompareUI.openCompareEditor(input);
    }
}
