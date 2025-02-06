/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ui.eclipse;

import java.util.Optional;

import org.e1c.edt.ai.CodeMethod;
import org.e1c.edt.ai.ICodeProvider;
import org.eclipse.xtext.parser.IParseResult;

class CodeProvider
    implements ICodeProvider
{
    private static final CodeMethod EmptyMethod = new CodeMethod("empty", 0, 0); //$NON-NLS-1$

    @Override
    public Optional<CodeMethod> getMethod(IParseResult parseResult, int offset)
    {
        return Optional.of(EmptyMethod);
    }

    @Override
    public Optional<String> getMethodBody(IParseResult parseResult, CodeMethod method)
    {
        return Optional.empty();
    }
}
