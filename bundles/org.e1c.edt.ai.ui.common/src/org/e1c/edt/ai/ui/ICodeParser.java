/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.time.Duration;
import java.util.Optional;

import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.xtext.parser.IParseResult;

public interface ICodeParser
{
    Optional<IParseResult> parse(SourceViewer sourceViewer);

    Optional<IParseResult> parse(SourceViewer sourceViewer, Duration timeout);
}
