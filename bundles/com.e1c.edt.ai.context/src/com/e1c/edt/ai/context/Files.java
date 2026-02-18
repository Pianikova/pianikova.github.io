/**
 *
 */
package com.e1c.edt.ai.context;

import java.io.File;
import java.util.Optional;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.core.filesystem.IQualifiedNameFilePathConverter;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com._1c.g5.v8.dt.metadata.mdclass.MdClassPackage;
import com._1c.g5.v8.dt.ui.util.LabelUtil;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.IFiles;
import com.e1c.edt.ai.IProjectTools;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;

public class Files
    implements IFiles
{
    private final Cache<String, String> displayedFileNameCache = CacheBuilder.newBuilder().maximumSize(256).build();
    private final IResourceLookup resourceLookup;
    private final IProjectTools projectTools;
    private final IBmPovider bmPovider;
    private final IQualifiedNameFilePathConverter fqn2PathConverter;

    @Inject
    public Files(IResourceLookup resourceLookup, IProjectTools projectTools, IBmPovider bmPovider,
        IQualifiedNameFilePathConverter fqn2PathConverter)
    {
        Preconditions.checkNotNull(resourceLookup);
        Preconditions.checkNotNull(projectTools);
        Preconditions.checkNotNull(bmPovider);
        Preconditions.checkNotNull(fqn2PathConverter);

        this.resourceLookup = resourceLookup;
        this.projectTools = projectTools;
        this.bmPovider = bmPovider;
        this.fqn2PathConverter = fqn2PathConverter;
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

    @Override
    @SuppressWarnings("nls")
    public String getDisplayedFileName(File file)
    {
        if (file == null)
        {
            return "";
        }

        var path = file.getAbsolutePath();
        var fromCache = displayedFileNameCache.getIfPresent(path);
        if (fromCache != null)
        {
            return fromCache;
        }

        var projectName = projectTools.determineProjectName(path);
        if (projectName == null || projectName.isBlank())
        {
            return getFileName(file).replace('/', '→');
        }

        if (path != projectName)
        {
            var root = ResourcesPlugin.getWorkspace().getRoot();
            var project = root.getProject(projectName);
            if (project != null)
            {
                var optionalProjectFile = projectTools.getProjectFile(project, path);
                if (optionalProjectFile.isPresent())
                {
                    var projectFile = optionalProjectFile.get();
                    try
                    {
                        var otionalRoot = bmPovider.getRoot(null,
                            project.getName() + "/" + projectFile.getProjectRelativePath().toPortableString(),
                            CancellationTokens.NONE);
                        if (otionalRoot.isPresent())
                        {
                            var objRoot = otionalRoot.get();
                            var label = LabelUtil.getPath(objRoot.getBmObject(), "→", IProject.class, 1, obj -> {
                                if (obj instanceof IProject)
                                {
                                    return false;
                                }

                                return true;
                            });

                            if (label != null && !label.isBlank())
                            {
                                // displayedFileNameCache.put(path, label);
                                return label;
                            }
                        }
                    }
                    catch (Throwable error)
                    {
                        //
                    }

                    var fqnName =
                        getFileName(projectFile)
                            .map(name -> name.replace('.', '→'));
                    if (fqnName.isPresent())
                    {
                        return fqnName.get();
                    }
                }
            }
        }

        return getFileName(file).replace('/', '→');
    }

    private Optional<String> getFileName(IFile file)
    {
        return Optional.ofNullable(fqn2PathConverter.getFqn(file)).map(fqn -> fqn.toString());
    }

    @SuppressWarnings("nls")
    private String getFileName(File file)
    {
        var path = file.getAbsolutePath();
        var fileName = file.getName();

        // Find the src directory in the path
        int srcIndex = path.indexOf("src");
        if (srcIndex > 0)
        {
            // Get the path after src (exclude "src" itself)
            var afterSrc = path.substring(srcIndex + 3); // +3 to skip "src"

            // Clean up path separators and leading slashes
            var relativePath = afterSrc.replace('\\', '/');
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
