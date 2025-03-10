/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.ui.editor.model.IXtextDocument;

import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IUISettings;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

class CodeParser
    implements ICodeParser
{
    private final ILog log;
    private final IDispatcher dispatcher;
    private final IUISettings settings;
    private final IClock clock;
    private final Cache<SourceViewer, Boolean> simpleModesCache =
        CacheBuilder.newBuilder().maximumSize(32).expireAfterWrite(15, TimeUnit.SECONDS).build();

    @Inject
    public CodeParser(ILog log, IDispatcher dispatcher, IUISettings settings, IClock clock)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(clock);
        this.log = log;
        this.settings = settings;
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    @Override
    public Optional<IParseResult> parse(SourceViewer sourceViewer)
    {
        Preconditions.checkNotNull(sourceViewer);
        return parse(sourceViewer, settings.getMinRequestDelay());
    }

    @SuppressWarnings("nls")
    private Optional<IParseResult> parse(SourceViewer sourceViewer, Duration timeout)
    {
        Preconditions.checkNotNull(sourceViewer);
        Preconditions.checkNotNull(timeout);
        var document = sourceViewer.getDocument();
        if (document.getLength() > 1024 * 1024)
        {
            log.trace("Code parser", () -> "The document is too large");
            return Optional.empty();
        }

        var simpleMode = simpleModesCache.getIfPresent(sourceViewer);
        if (simpleMode != null && simpleMode == true)
        {
            return Optional.empty();
        }

        var startTime = clock.now();
        var result = Optional.ofNullable((document instanceof IXtextDocument) ? (IXtextDocument)document : null)
            .flatMap(
                xtextDocument -> dispatcher.dispatch(() -> xtextDocument.readOnly(s -> s.getParseResult()), timeout));

        simpleMode = result.map(i -> false).orElse(true);
        if (simpleMode)
        {
            simpleModesCache.put(sourceViewer, simpleMode);
            log.trace("Code parser", () -> "Unable to parse during " + timeout);
        }
        else
        {
            var duration = Duration.between(startTime, clock.now());
            log.trace("Code parser", () -> "The duration of the parsing is " + duration);
        }

        return result;
    }
}
