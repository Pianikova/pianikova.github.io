/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.context.ModuleInfo;
import org.eclipse.jface.text.IDocument;

public interface IModuleProvider
{
    Optional<ModuleInfo> getModule(String filePath, ICancellationToken cancellationToken);

    Optional<ModuleInfo> getModuleInfo(IDocument document);
}
