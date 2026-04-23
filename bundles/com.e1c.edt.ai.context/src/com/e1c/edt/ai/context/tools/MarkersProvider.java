/**
 *
 */
package com.e1c.edt.ai.context.tools;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.core.IBmTransaction;
import com._1c.g5.v8.bm.integration.IBmTask;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.validation.marker.BmObjectMarker;
import com._1c.g5.v8.dt.validation.marker.Marker;
import com._1c.g5.v8.dt.validation.marker.v2.IMarkerManagerV2;
import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IMarkersProvider;
import com.e1c.edt.ai.assistent.model.MarkerInfo;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class MarkersProvider implements IMarkersProvider
{
    private final IMarkerManagerV2 markerManager;
    private final IContentSourceProvider contentSourceProvider;
    private final IBmModelManager modelManager;
    private final IProjectFileSystemSupportProvider projectFileSystemSupportProvider;

    @Inject
    public MarkersProvider(IMarkerManagerV2 markerManager, IContentSourceProvider contentSourceProvider,
        IBmModelManager modelManager, IProjectFileSystemSupportProvider projectFileSystemSupportProvider)
    {
        Preconditions.checkNotNull(markerManager);
        Preconditions.checkNotNull(contentSourceProvider);
        Preconditions.checkNotNull(modelManager);
        Preconditions.checkNotNull(projectFileSystemSupportProvider);

        this.markerManager = markerManager;
        this.contentSourceProvider = contentSourceProvider;
        this.modelManager = modelManager;
        this.projectFileSystemSupportProvider = projectFileSystemSupportProvider;
    }

    @Override
    public Stream<MarkerInfo> getMarkers(IProject project, IFile file)
    {
        var reader = markerManager.createReader(project);
        return reader.markers()
            .map(marker -> createMarkerInfo(project, file, marker))
            .filter(marker -> marker.isPresent())
            .map(marker -> marker.get());
    }

    @SuppressWarnings({ "nls", "unchecked" })
    private Optional<MarkerInfo> createMarkerInfo(IProject project, IFile targetFile, Marker marker)
    {
        var info = new MarkerInfo();
        info.type = "1c"; //$NON-NLS-1$
        info.message = marker.getMessage();
        info.sourceId = marker.getSourceType();
        var details = new HashMap<String, Object>();
        info.details = details;
        Integer offset = null;
        Integer length = null;

        // Use reflection to handle both Map<String, String> and IExtraInfoMap return types
        Map<String, String> extraInfos = null;
        try
        {
            var getExtraInfoMethod = marker.getClass().getMethod("getExtraInfo");
            var result = getExtraInfoMethod.invoke(marker);
            if (result instanceof Map)
            {
                extraInfos = (Map<String, String>)result;
            }
        }
        catch (Exception e)
        {
            // Method not available or reflection error
            extraInfos = null;
        }

        if (extraInfos != null)
        {
            for (var extraInfo : extraInfos.entrySet())
            {
                var key = extraInfo.getKey();
                if (key == null || key.isBlank())
                {
                    continue;
                }

                var value = extraInfo.getValue();
                if (value == null || value.isBlank())
                {
                    continue;
                }

                if ("line".equalsIgnoreCase(key))
                {
                    try
                    {
                        info.startLine = Integer.parseInt(value);
                        continue;
                    }
                    catch (NumberFormatException ex)
                    {
                        //
                    }
                }

                if ("offset".equalsIgnoreCase(key))
                {
                    try
                    {
                        offset = Integer.parseInt(value);
                        continue;
                    }
                    catch (NumberFormatException ex)
                    {
                        //
                    }
                }

                if ("length".equalsIgnoreCase(key))
                {
                    try
                    {
                        length = Integer.parseInt(value);
                        continue;
                    }
                    catch (NumberFormatException ex)
                    {
                        //
                    }
                }

                details.put(key, value);
            }
        }

        details.put("1c_check_id", marker.getCheckId());
        details.put("1c_top_object_id", marker.getTopObjectId());

        switch (marker.getSeverity())
        {
        case BLOCKER:
            info.severity = MarkerInfo.SEVERITY_ERROR;
            info.priority = MarkerInfo.PRIORITY_HIGH;
            break;

        case CRITICAL:
            info.severity = MarkerInfo.SEVERITY_ERROR;
            info.priority = MarkerInfo.PRIORITY_HIGH;
            break;

        case ERRORS:
            info.severity = MarkerInfo.SEVERITY_ERROR;
            info.priority = MarkerInfo.PRIORITY_NORMAL;
            break;

        case MAJOR:
            info.severity = MarkerInfo.SEVERITY_ERROR;
            info.priority = MarkerInfo.PRIORITY_LOW;
            break;

        case MINOR:
            info.severity = MarkerInfo.SEVERITY_WARNING;
            info.priority = MarkerInfo.PRIORITY_NORMAL;
            break;

        case TRIVIAL:
            info.severity = MarkerInfo.SEVERITY_INFO;
            info.priority = MarkerInfo.PRIORITY_HIGH;
            break;

        case NONE:
            info.severity = MarkerInfo.SEVERITY_INFO;
            info.priority = MarkerInfo.PRIORITY_NORMAL;
            break;

        default:
            info.severity = MarkerInfo.SEVERITY_INFO;
            info.priority = MarkerInfo.PRIORITY_LOW;
            break;
        }

        if (marker instanceof BmObjectMarker)
        {
            var objectMarker = (BmObjectMarker)marker;
            details.put("1c_object_id", objectMarker.getObjectId());
        }

        /*if (marker instanceof PlainEObjectMarker)
        {
            var objectMarker = (PlainEObjectMarker)marker;
        }*/

        var topObjectId = marker.getTopObjectId();
        IFile file = null;
        if (topObjectId instanceof String)
        {
            file = getFileByPath(project, (String)topObjectId);
        }
        else if (topObjectId instanceof Long)
        {
            file = getFileByObjectId(project, (Long)topObjectId);
        }

        // Set info.path and info.location if file was found
        if (file != null)
        {
            // Filter by targetFile if specified
            if (targetFile != null && !targetFile.equals(file))
            {
                return Optional.empty();
            }

            var location = file.getLocation();
            if (location != null)
            {
                info.path = location.toOSString();
            }

            if (info.startLine != null)
            {
                info.location = "line: " + info.startLine;
                if (info.path != null)
                {
                    info.location = info.location + " " + info.path;
                }

                // Read target content if positions are available
                if (offset != null && length != null && file.exists())
                {
                    try
                    {
                        info.markerHighlightedText = readContentFromFile(file, offset, length);
                    }
                    catch (Exception e)
                    {
                        //
                    }
                }
            }
        }

        return Optional.ofNullable(info);
    }

    private String readContentFromFile(IFile file, int offset, int length)
        throws org.eclipse.jface.text.BadLocationException
    {
        var optionalDocument = contentSourceProvider.getFileDocument(file);
        if (optionalDocument.isEmpty())
        {
            return null;
        }

        var document = optionalDocument.get();
        var doc = document.getDocument();
        return doc.get(offset, length);
    }

    @SuppressWarnings("nls")
    private IFile getFileByPath(IProject project, String path)
    {
        // Remove project name prefix if present (e.g., "/MyProject/a/b/c" -> "a/b/c")
        if (path.startsWith("/" + project.getName() + "/"))
        {
            path = path.substring(project.getName().length() + 2);
        }

        return project.getFile(path);
    }

    private IFile getFileByObjectId(IProject project, Long objectId)
    {
        var model = modelManager.getModel(project);
        if (model == null)
        {
            return null;
        }

        try
        {
            var bmObject = model.executeReadonlyTask(new IBmTask<IBmObject>()
            {
                @Override
                public IBmObject execute(IBmTransaction transaction,
                    org.eclipse.core.runtime.IProgressMonitor progressMonitor)
                {
                    return transaction.getObjectById(objectId);
                }

                @Override
                public Object getId()
                {
                    return "MarkersProvider/" + objectId; //$NON-NLS-1$
                }

                @Override
                public String getName()
                {
                    return "Get object by id: " + objectId; //$NON-NLS-1$
                }

                @Override
                public Object getServiceId()
                {
                    return "MarkersProvider"; //$NON-NLS-1$
                }
            });

            if (bmObject != null)
            {
                var fileSystem = projectFileSystemSupportProvider.getProjectFileSystemSupport(project);
                if (fileSystem != null)
                {
                    var file = fileSystem.getFile(bmObject);
                    if (file != null)
                    {
                        return file;
                    }
                }
            }
        }
        catch (Exception e)
        {
            // Failed to get file from Long topObjectId, return null
        }

        return null;
    }
}
