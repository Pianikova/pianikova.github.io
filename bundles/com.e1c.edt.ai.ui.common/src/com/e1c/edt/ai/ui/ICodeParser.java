/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.parser.IParseResult;

public interface ICodeParser
{
    Optional<IParseResult> parse(SourceViewer sourceViewer);
}
