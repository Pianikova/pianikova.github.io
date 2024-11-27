/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import java.util.Optional;

import org.e1c.edt.ai.CodeMethod;
import org.e1c.edt.ai.ICodeProvider;
import org.eclipse.xtext.parser.IParseResult;

class CodeProvider
    implements ICodeProvider
{
    @Override
    public Optional<CodeMethod> getMethod(IParseResult parseResult, int offset)
    {
        return Optional.empty();
    }

    @Override
    public Optional<String> getMethodBody(IParseResult parseResult, CodeMethod method)
    {
        return Optional.empty();
    }
}
