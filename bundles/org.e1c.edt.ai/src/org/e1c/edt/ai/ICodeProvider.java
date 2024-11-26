/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

import org.eclipse.xtext.parser.IParseResult;

public interface ICodeProvider
{
    Optional<CodeMethod> getMethod(IParseResult parseResult, int offset);

    Optional<String> getMethodBody(IParseResult parseResult, CodeMethod method);
}
