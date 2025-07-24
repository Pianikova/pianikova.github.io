/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ui.eclipse;

import java.util.Optional;

import com.e1c.edt.ai.ui.IModuleNameProvider;

/**
 * @author Bogdan Sushkov
 *
 */
public class ModuleNameProvider
    implements IModuleNameProvider
{

    @Override
    public Optional<String> getModuleName(String path)
    {
        return Optional.of(path);
    }

}
