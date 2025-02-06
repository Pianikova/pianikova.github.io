/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ui.eclipse;

import java.time.Duration;
import java.util.Optional;

import org.e1c.edt.ai.ui.ICodeParser;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.nodemodel.INode;
import org.eclipse.xtext.parser.IParseResult;

class CodeParser
    implements ICodeParser
{
    private static final ParseResult EmptyParseResult = new ParseResult();

    @Override
    public Optional<IParseResult> parse(SourceViewer sourceViewer)
    {
        return Optional.of(EmptyParseResult);
    }

    @Override
    public Optional<IParseResult> parse(SourceViewer sourceViewer, Duration timeout)
    {
        return Optional.of(EmptyParseResult);
    }

    private static class ParseResult
        implements IParseResult
    {
        @Override
        public EObject getRootASTElement()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public ICompositeNode getRootNode()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public Iterable<INode> getSyntaxErrors()
        {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public boolean hasSyntaxErrors()
        {
            // TODO Auto-generated method stub
            return false;
        }
    }
}
