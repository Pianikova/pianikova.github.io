/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.util.Optional;

/**
 * @author Bogdan Sushkov
 *
 */
public interface IModuleNameProvider
{
    /**
     * Get module name by path.
     * @param path - path to module
     * @return optional module name
     */
    Optional<String> getModuleName(String path);
}
