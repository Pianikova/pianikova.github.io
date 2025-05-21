/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.Optional;

import org.eclipse.jface.text.IDocument;

import com.e1c.edt.ai.ICancellationToken;

public interface IModuleProvider
{
    Optional<ModuleInfo> getModule(IDocument document, String filePath, ICancellationToken cancellationToken);

    Optional<ModuleInfo> getModuleInfo(IDocument document, ICancellationToken cancellationToken);
}
