/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai;

import java.util.Optional;

public interface IProgramingLanguage
{
    Optional<String> getFromPath(String filePath);
}
