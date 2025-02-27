/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.semantic;

import java.util.Optional;

import org.e1c.edt.ai.IProgramingLanguage;

public class ProgramingLanguage
    implements IProgramingLanguage
{
    @Override
    public Optional<String> getFromPath(String filePath)
    {
        return Optional.of("bsl"); //$NON-NLS-1$
    }
}
