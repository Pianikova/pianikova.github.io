/**
 *
 */
package com.e1c.edt.ai.context;

import java.util.Optional;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.core.model.EditingMode;
import com._1c.g5.v8.dt.core.model.IModelEditingSupport;
import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.ObjectBelonging;
import com.e1c.edt.ai.IEditingSupport;
import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class EditingSupport
    implements IEditingSupport
{
    @SuppressWarnings("nls")
    private static final Set<String> RESTRICTED_EXTENSIONS = Set.of(".form", ".mdo", ".dcs", ".mxl", ".mxlx");

    private final IBmObjectProvider bmObjectProvider;
    private final IModelEditingSupport modelEditingSupport;
    private final IV8ProjectManager v8ProjectManager;
    private final ILog log;

    @Inject
    public EditingSupport(IBmObjectProvider bmObjectProvider, IModelEditingSupport modelEditingSupport,
        IV8ProjectManager v8ProjectManager, ILog log)
    {
        Preconditions.checkNotNull(bmObjectProvider);
        Preconditions.checkNotNull(modelEditingSupport);
        Preconditions.checkNotNull(v8ProjectManager);
        Preconditions.checkNotNull(log);

        this.bmObjectProvider = bmObjectProvider;
        this.modelEditingSupport = modelEditingSupport;
        this.v8ProjectManager = v8ProjectManager;
        this.log = log;
    }

    @Override
    public boolean canEdit(IFile file)
    {
        return !isReadOnly(file.getProject()) && getObject(file).map(obj -> canEdit(obj)).orElse(true);
    }

    @Override
    public boolean canCreate(IFile file)
    {
        return !isReadOnly(file.getProject()) && !isRestrictedFile(file)
            && getObject(file).map(obj -> canEdit(obj)).orElse(true);
    }

    @Override
    public boolean canDelete(IFile file)
    {
        return !isReadOnly(file.getProject()) && !isRestrictedFile(file)
            && getObject(file).map(obj -> canDelete(obj)).orElse(true);
    }

    @Override
    public boolean isReadOnly(IProject project)
    {
        if (project == null || !project.isAccessible())
        {
            return false;
        }

        try
        {
            var v8Project = v8ProjectManager.getProject(project);
            if (v8Project instanceof IConfigurationProject)
            {
                var configuration = ((IConfigurationProject)v8Project).getConfiguration();
                return configuration != null && configuration.getObjectBelonging() == ObjectBelonging.ADOPTED;
            }
        }
        catch (RuntimeException error)
        {
            // Fail open: treat indeterminate state as writable, this is an LLM guidance
            // guard, not a security boundary.
            log.logError(error);
        }

        return false;
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
