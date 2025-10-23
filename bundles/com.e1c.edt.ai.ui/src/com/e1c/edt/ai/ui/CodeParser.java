/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.Optional;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.util.concurrent.IUnitOfWork;

import com._1c.g5.v8.dt.bsl.ui.editor.BslXtextDocument;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.TracingSources;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class CodeParser
    implements ICodeParser
{
    private final ILog log;
    private final IDispatcher dispatcher;
    private final IClock clock;

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
        dispatcher.checkThread(false, false);
        var document = sourceViewer.getDocument();
        var startTime = clock.now();
        if (document instanceof BslXtextDocument)
        {
            var bslDocument = (BslXtextDocument)document;
            var pareseResult = bslDocument.readOnlyDataModelWithoutSync(new IUnitOfWork<IParseResult, XtextResource>()
            {
                @Override
                public IParseResult exec(XtextResource state) throws Exception
                {
                    var result = state.getParseResult();
                    if (result == null || result.getRootASTElement() == null)
                    {
                        return null;
                    }

                    return result;
                }
            });

            log.trace(TracingSources.COMMON, "Code parser",
                () -> "The duration of the parsing is " + Duration.between(startTime, clock.now()));
            return Optional.ofNullable(pareseResult);
        }

        return Optional.empty();
    }
}
