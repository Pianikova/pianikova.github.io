/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ui.eclipse;

import java.util.Optional;

import org.e1c.edt.ai.ICodePartsProvider;
import org.e1c.edt.ai.ICursorInfoProvider;
import org.e1c.edt.ai.Range;
import org.e1c.edt.ai.assistent.model.CursorInfo;
import org.e1c.edt.ai.assistent.model.RelativeLocation;
import org.e1c.edt.ai.ui.IUI;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.nodemodel.util.NodeModelUtils;
import org.eclipse.xtext.ui.editor.XtextSourceViewer;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CursorInfoProvider
    implements ICursorInfoProvider
{
    private final IUI ui;
    private final ICodePartsProvider codePartsProvider;

    @Inject
    public CursorInfoProvider(IUI ui, ICodePartsProvider codePartsProvider)
    {
        Preconditions.checkNotNull(ui);
        Preconditions.checkNotNull(codePartsProvider);
        this.ui = ui;
        this.codePartsProvider = codePartsProvider;
    }

    @Override
    public Optional<CursorInfo> getCursorInfo(int cursorOffset)
    {
        return ui.getTextWidget()
            .flatMap(textWidget -> ui.getSourceViewer(textWidget))
            .map(sourceViewer -> getCursorInfo(cursorOffset, sourceViewer));
    }

    private CursorInfo getCursorInfo(int cursorOffset, SourceViewer sourceViewer)
    {
        if (!(sourceViewer instanceof XtextSourceViewer))
        {
            return null;
        }

        var xtextSourceViewer = (XtextSourceViewer)sourceViewer;
        var document = xtextSourceViewer.getXtextDocument();
        if (document == null)
        {
            return null;
        }

        var parseResult = document.readOnly(s -> s.getParseResult());
        if (parseResult == null)
        {
            return null;
        }

        var rootNoode = parseResult.getRootNode();
        if (rootNoode == null)
        {
            return null;
        }

        var cursorNode = NodeModelUtils.findLeafNodeAtOffset(rootNoode, cursorOffset);
        if (cursorNode == null)
        {
            return null;
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
            return null;
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

        return cursorInfo;
    }
}
