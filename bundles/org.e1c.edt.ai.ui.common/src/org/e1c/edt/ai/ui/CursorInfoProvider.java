/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.ICodePartsProvider;
import org.e1c.edt.ai.ICursorInfoProvider;
import org.e1c.edt.ai.Range;
import org.e1c.edt.ai.assistent.model.CursorInfo;
import org.e1c.edt.ai.assistent.model.RelativeLocation;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CursorInfoProvider
    implements ICursorInfoProvider
{
    private final IUI ui;
    private final ICodePartsProvider codePartsProvider;
    private final ICodeParser codeParser;

    @Inject
    public CursorInfoProvider(IUI ui, ICodePartsProvider codePartsProvider, ICodeParser codeParser)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(codePartsProvider);
        Preconditions.checkNotNull(codeParser);
        this.ui = ui;
        this.codePartsProvider = codePartsProvider;
        this.codeParser = codeParser;
    }

    @Override
    public Optional<CursorInfo> getCursorInfo(int cursorOffset)
    {
        return ui.getTextWidget()
            .flatMap(textWidget -> ui.getSourceViewer(textWidget))
            .flatMap(sourceViewer -> getCursorInfo(cursorOffset, sourceViewer));
    }

    private Optional<CursorInfo> getCursorInfo(int cursorOffset, SourceViewer sourceViewer)
    {
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
