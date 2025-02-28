/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.semantic;

import java.util.Optional;

import com.e1c.edt.ai.IProgramingLanguage;

public class ProgramingLanguage
    implements IProgramingLanguage
{
    @Override
    public Optional<String> getFromPath(String filePath)
    {
        return Optional.of("bsl"); //$NON-NLS-1$
    }
}
