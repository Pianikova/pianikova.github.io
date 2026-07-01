/**
 *
 */
package com.e1c.edt.ai;

import java.io.File;
import java.util.Optional;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;

public interface IFiles
{
    Optional<IFile> getCodeFile(EObject eObject);

    /**
     * Returns the on-disk source folder of a metadata object (e.g. a Document, Catalog, Form or
     * Template). The folder holds all files that belong to the object — its {@code .mdo}, BSL
     * modules, forms, templates, commands, etc. — so callers can add the whole object to the chat.
     *
     * @param eObject a metadata object (typically an {@code MdObject} dragged from the navigator)
     * @return the object's folder, or empty if it cannot be resolved (e.g. non-EDT variant)
     */
    Optional<IContainer> getObjectFolder(EObject eObject);

    String getDisplayedFileName(File file);
}
