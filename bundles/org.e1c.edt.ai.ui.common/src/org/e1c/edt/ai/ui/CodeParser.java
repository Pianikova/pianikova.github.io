/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.Optional;

import org.e1c.edt.ai.IUISettings;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class CodeParser
    implements ICodeParser
{
    private final IDispatcher dispatcher;
    private final IUISettings settings;

    @Inject
    public CodeParser(IDispatcher dispatcher, IUISettings settings)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(settings);
        this.settings = settings;
        this.dispatcher = dispatcher;
    }

    @Override
    public Optional<IParseResult> parse(SourceViewer sourceViewer)
    {
        Preconditions.checkNotNull(sourceViewer);
        return parse(sourceViewer, settings.getMinRequestDelay());
    }

    @Override
    public Optional<IParseResult> parse(SourceViewer sourceViewer, Duration timeout)
    {
        Preconditions.checkNotNull(sourceViewer);
        Preconditions.checkNotNull(timeout);
        var document = sourceViewer.getDocument();
        return Optional.ofNullable((document instanceof IXtextDocument) ? (IXtextDocument)document : null)
            .flatMap(
                xtextDocument -> dispatcher.dispatch(() -> xtextDocument.readOnly(s -> s.getParseResult()), timeout));
    }
}
