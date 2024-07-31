/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.CodeMethod;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.xtext.parser.IParseResult;

public interface ICodeProvider
{
    Optional<IParseResult> getParseResult(StyledText textWidget);

    Optional<CodeMethod> getMethod(IParseResult parseResult, int offset);

    Optional<String> getMethodBody(IParseResult parseResult, CodeMethod method);
}
