/**
 *
 */
package com.e1c.edt.ai.context;

import java.util.Optional;
import java.util.Set;

import org.eclipse.core.resources.IFile;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.core.model.EditingMode;
import com._1c.g5.v8.dt.core.model.IModelEditingSupport;
import com.e1c.edt.ai.IEditingSupport;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class EditingSupport
    implements IEditingSupport
{
    @SuppressWarnings("nls")
    private static final Set<String> RESTRICTED_EXTENSIONS = Set.of(".form", ".mdo");

    private final IBmObjectProvider bmObjectProvider;
    private final IModelEditingSupport modelEditingSupport;

    @Inject
    public EditingSupport(IBmObjectProvider bmObjectProvider, IModelEditingSupport modelEditingSupport)
    {
        Preconditions.checkNotNull(bmObjectProvider);
        Preconditions.checkNotNull(modelEditingSupport);

        this.bmObjectProvider = bmObjectProvider;
        this.modelEditingSupport = modelEditingSupport;
    }

    @Override
    public boolean canEdit(IFile file)
    {
        return !isRestrictedFile(file) && getObject(file).map(obj -> canEdit(obj)).orElse(true);
    }

    @Override
    public boolean canDelete(IFile file)
    {
        return !isRestrictedFile(file) && getObject(file).map(obj -> canDelete(obj)).orElse(true);
    }

    private boolean isRestrictedFile(IFile file)
    {
        var fileName = file.getName();
        var lastDotIndex = fileName.lastIndexOf('.');
        if (lastDotIndex < 0)
        {
            return false;
        }

        var extension = fileName.substring(lastDotIndex);
        return RESTRICTED_EXTENSIONS.contains(extension);
    }

    private boolean canEdit(IBmObject obj)
    {
        return modelEditingSupport.canEdit(obj, EditingMode.DIRECT);
    }

    private boolean canDelete(IBmObject obj)
    {
        return modelEditingSupport.canDelete(obj, EditingMode.DIRECT);
    }

    private Optional<IBmObject> getObject(IFile file)
    {
        return bmObjectProvider.getObject(file);
    }
}
