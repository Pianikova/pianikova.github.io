/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai;

import java.util.Optional;

import com.google.common.io.Files;

class ProgramingLanguage
    implements IProgramingLanguage
{
    @SuppressWarnings("nls")
    @Override
    public Optional<String> getFromPath(String filePath)
    {
        var ext = Files.getFileExtension(filePath);
        if (ext == null || ext.isBlank())
        {
            return Optional.empty();
        }

        ext = ext.toLowerCase();
        switch (ext)
        {
        case "bsl":
            ext = "1с";
            break;
        }

        return Optional.of(ext);
    }
}
