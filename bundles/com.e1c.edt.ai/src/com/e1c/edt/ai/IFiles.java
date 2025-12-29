/**
 *
 */
package com.e1c.edt.ai;

import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;

public interface IFiles
{
    Optional<IFile> getCodeFile(EObject eObject);
}
