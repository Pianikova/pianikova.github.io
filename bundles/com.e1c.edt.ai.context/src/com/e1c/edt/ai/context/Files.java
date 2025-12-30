/**
 *
 */
package com.e1c.edt.ai.context;

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
}
