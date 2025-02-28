/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ui.eclipse;

import java.util.Optional;

import com.e1c.edt.ai.CodeMethod;
import com.e1c.edt.ai.ICodeProvider;
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
