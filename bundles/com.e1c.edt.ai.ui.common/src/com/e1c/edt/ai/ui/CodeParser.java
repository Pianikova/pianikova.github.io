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
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

class CodeParser
    implements ICodeParser
{
    private final ILog log;
    private final IDispatcher dispatcher;
    private final IClock clock;
    private final Cache<SourceViewer, Boolean> simpleModesCache =
        CacheBuilder.newBuilder().maximumSize(32).expireAfterWrite(15, TimeUnit.SECONDS).build();

    @Inject
    public CodeParser(ILog log, IDispatcher dispatcher, IClock clock)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(clock);
        this.log = log;
        this.dispatcher = dispatcher;
        this.clock = clock;
    }

    @SuppressWarnings("nls")
    @Override
    public Optional<IParseResult> parse(SourceViewer sourceViewer)
    {
        Preconditions.checkNotNull(sourceViewer);
        if (!dispatcher.checkThread(false, false) && sourceViewer.getDocument().getLength() > Consts.NORMAL_CODE_SIZE)
        {
            log.warning("Code parser", () -> "The document is too large");
            return Optional.empty();
        }

        var document = sourceViewer.getDocument();
        var simpleMode = simpleModesCache.getIfPresent(sourceViewer);
        if (simpleMode != null && simpleMode == true)
        {
            return Optional.empty();
        }

        var startTime = clock.now();
        var result = Optional.ofNullable((document instanceof IXtextDocument) ? (IXtextDocument)document : null)
            .map(xtextDocument -> xtextDocument.readOnly(s -> s.getParseResult()));

        simpleMode = result.map(i -> false).orElse(true);
        if (simpleMode)
        {
            simpleModesCache.put(sourceViewer, simpleMode);
            log.warning("Code parser", () -> "Unable to parse");
        }
        else
        {
            var duration = Duration.between(startTime, clock.now());
            log.debug("Code parser", () -> "The duration of the parsing is " + duration);
        }

        return result;
    }
}
