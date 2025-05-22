/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com.e1c.edt.ai.ICodePartsProvider;
import com.e1c.edt.ai.ICursorInfoProvider;
import com.e1c.edt.ai.Range;
import com.e1c.edt.ai.assistent.model.CursorInfo;
import com.e1c.edt.ai.assistent.model.RelativeLocation;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CursorInfoProvider
    implements ICursorInfoProvider
{
    private final IDispatcher dispatcher;
    private final IUI ui;
    private final ICodePartsProvider codePartsProvider;
    private final ICodeParser codeParser;

    @Inject
    public CursorInfoProvider(IDispatcher dispatcher, IUI ui, ICodePartsProvider codePartsProvider,
        ICodeParser codeParser)
    {
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(codePartsProvider);
        Preconditions.checkNotNull(codeParser);
        this.dispatcher = dispatcher;
        this.ui = ui;
        this.codePartsProvider = codePartsProvider;
        this.codeParser = codeParser;
    }

    @Override
    public Optional<CursorInfo> getCursorInfo(int cursorOffset)
    {
        return dispatcher.dispatch(() -> ui.getTextWidget().flatMap(textWidget -> ui.getSourceViewer(textWidget)))
            .flatMap(i -> i)
            .flatMap(sourceViewer -> getCursorInfo(cursorOffset, sourceViewer));
    }

    private Optional<CursorInfo> getCursorInfo(int cursorOffset, SourceViewer sourceViewer)
    {
        if (sourceViewer.getDocument().getLength() > Consts.NORMAL_CODE_SIZE)
        {
            return Optional.empty();
        }

        var rootNoodeOptional = codeParser.parse(sourceViewer).map(parseResult -> parseResult.getRootNode());
        if (rootNoodeOptional.isEmpty())
        {
            return Optional.empty();
        }

        var rootNoode = rootNoodeOptional.get();
        var cursorNode = NodeModelUtils.findLeafNodeAtOffset(rootNoode, cursorOffset);
        if (cursorNode == null)
        {
            return Optional.empty();
        }

        var cursorInfo = new CursorInfo();
        Range range = Range.EMPTY;
        boolean found = false;

        var partsIterator = codePartsProvider.getParts(rootNoode).iterator();
        while (partsIterator.hasNext())
        {
            var part = partsIterator.next();
            if (!part.getLocation().equals(cursorInfo.location))
            {
                if (found)
                {
                    break;
                }

                cursorInfo.location = part.getLocation();
                range = part.getRange();
            }
            else
            {
                range = range.merge(part.getRange());
            }

            if (range.contains(cursorOffset))
            {
                found = true;
            }
        }

        if (!found)
        {
            return Optional.empty();
        }

        var relativeCursorOffset = cursorOffset - range.getStart();
        var normalizedCursorOffset = (double)relativeCursorOffset / (double)range.getLength();
        cursorInfo.relativeLocation = RelativeLocation.Middle;
        if (normalizedCursorOffset <= .2)
        {
            cursorInfo.relativeLocation = RelativeLocation.Start;
        }
        else
        {
            if (normalizedCursorOffset >= .8)
            {
                cursorInfo.relativeLocation = RelativeLocation.End;
            }
        }

        return Optional.of(cursorInfo);
    }
}
