/**
 *
 */
package com.e1c.edt.ui.eclipse;

import java.io.File;
import java.util.Optional;

import org.eclipse.core.resources.IContainer;
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

    @Override
    public Optional<IContainer> getObjectFolder(EObject eObject)
    {
        return Optional.empty();
    }

    @Override
    public String getDisplayedFileName(File file)
    {
        if (file == null)
        {
            return ""; //$NON-NLS-1$
        }

        return file.getName();
    }
}
