/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.Optional;

import com.e1c.edt.ai.ICancellationToken;
import org.eclipse.jface.text.IDocument;

public interface IModuleProvider
{
    Optional<ModuleInfo> getModule(String filePath, ICancellationToken cancellationToken);

    Optional<ModuleInfo> getModuleInfo(IDocument document, ICancellationToken cancellationToken);
}
