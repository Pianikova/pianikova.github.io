/**
 *
 */
package com.e1c.edt.ai.context;

import java.io.File;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com.e1c.edt.ai.IFiles;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class Files
    implements IFiles
{
    private final IResourceLookup resourceLookup;

    @Inject
    public Files(IResourceLookup resourceLookup)
    {
        Preconditions.checkNotNull(resourceLookup);
        this.resourceLookup = resourceLookup;
    }

    @Override
    public Optional<IFile> getCodeFile(EObject eObject)
    {
        var module = eObject.eGet(MdClassPackage.Literals.COMMON_MODULE__MODULE, true);
        if (module instanceof Module)
        {
            var file = resourceLookup.getPlatformResource((Module)module);
            if (file != null && !file.isHidden() && !file.isVirtual() && file.exists())
            {
                return Optional.of(file);
            }
        }

        return Optional.empty();
    }

    @SuppressWarnings("nls")
    @Override
    public String getDisplayedFileName(File file)
    {
        if (file == null)
        {
            return "";
        }

        String path = file.getPath();
        String fileName = file.getName();

        // Find the src directory in the path
        int srcIndex = path.indexOf("src");
        if (srcIndex > 0)
        {
            // Get the path after src (exclude "src" itself)
            String afterSrc = path.substring(srcIndex + 3); // +3 to skip "src"

            // Clean up path separators and leading slashes
            String relativePath = afterSrc.replace('\\', '/');
            while (relativePath.startsWith("/"))
            {
                relativePath = relativePath.substring(1);
            }

            // Check if the relative path already contains the file name
            if (!relativePath.isEmpty() && relativePath.endsWith(fileName))
            {
                // Already contains the file name, just return the relative path
                return relativePath;
            }
            else if (!relativePath.isEmpty())
            {
                // Doesn't contain the file name, add it
                return relativePath + "/" + fileName;
            }
        }

        // If src not found, return just the file name
        return fileName;
    }
}
