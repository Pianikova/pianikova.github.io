/**
 *
 */
package com.e1c.edt.ui.eclipse;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;

import com.e1c.edt.ai.IFiles;

public class Files
    implements IFiles
{
    @Override
    public Optional<IFile> getCodeFile(EObject eObject)
    {
        return Optional.empty();
    }

}
