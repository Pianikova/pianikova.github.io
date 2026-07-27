/**
 *
 */
package com.e1c.edt.ai.context;

import java.util.Optional;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.dt.core.model.EditingMode;
import com._1c.g5.v8.dt.core.model.IModelEditingSupport;
import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
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
                // Same path the EDT editors use: the distribution provider forbids editing
                // when the configuration is on full vendor support (ParentConfigurations.bin).
                return configuration != null && !modelEditingSupport.canEdit(configuration, EditingMode.DIRECT);
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

    @Override
    public boolean canEdit(Object object)
    {
        if (!(object instanceof EObject))
        {
            // Nothing to judge (null, or not a model object): let the caller proceed, the
            // project-level check still applies.
            return true;
        }
        try
        {
            return modelEditingSupport.canEdit((EObject)object, EditingMode.DIRECT);
        }
        catch (RuntimeException error)
        {
            log.logError(error);
            return true;
        }
    }

    @Override
    public boolean canDelete(Object object)
    {
        if (!(object instanceof EObject))
        {
            return true;
        }
        try
        {
            return modelEditingSupport.canDelete((EObject)object, EditingMode.DIRECT);
        }
        catch (RuntimeException error)
        {
            log.logError(error);
            return true;
        }
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
