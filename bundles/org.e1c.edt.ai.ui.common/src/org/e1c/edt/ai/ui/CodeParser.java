/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.e1c.edt.ai.IUISettings;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;

import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

class CodeParser
    implements ICodeParser
{
    private final IDispatcher dispatcher;
    private final IUISettings settings;
    private final Cache<String, Optional<IParseResult>> parseCache =
        CacheBuilder.newBuilder().maximumSize(8).expireAfterWrite(15, TimeUnit.MINUTES).build();

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
    public synchronized Optional<IParseResult> parse(SourceViewer sourceViewer, Duration timeout)
    {
        Preconditions.checkNotNull(sourceViewer);
        Preconditions.checkNotNull(timeout);
        var optionalText = dispatcher.dispatch(() -> sourceViewer.getTextWidget().getText());
        if (optionalText.isEmpty())
        {
            return Optional.empty();
        }

        var text = optionalText.get();
        var result = parseCache.getIfPresent(text);
        if (result != null)
        {
            return result;
        }

        var document = sourceViewer.getDocument();
        result = Optional.ofNullable((document instanceof IXtextDocument) ? (IXtextDocument)document : null)
            .flatMap(
                xtextDocument -> dispatcher.dispatch(() -> xtextDocument.readOnly(s -> s.getParseResult()), timeout));

        parseCache.put(text, result);
        return result;
    }
}
