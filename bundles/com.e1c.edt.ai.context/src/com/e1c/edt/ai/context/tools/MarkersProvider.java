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

    @Inject
    public MarkersProvider(IMarkerManagerV2 markerManager, IContentSourceProvider contentSourceProvider)
    {
        Preconditions.checkNotNull(markerManager);
        Preconditions.checkNotNull(contentSourceProvider);

        this.markerManager = markerManager;
        this.contentSourceProvider = contentSourceProvider;
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

        // Read target content if positions are available
        if (info.startLine != null && offset != null && length != null)
        {
            try
            {
                var topObjectId = marker.getTopObjectId();
                if (topObjectId instanceof String)
                {
                    var path = (String)topObjectId;
                    info.location = "line: " + info.startLine + " " + path;
                    // Remove project name prefix if present (e.g., "/MyProject/a/b/c" -> "a/b/c")
                    if (path.startsWith("/" + project.getName() + "/"))
                    {
                        path = path.substring(project.getName().length() + 2);
                    }

                    var file = project.getFile(path);
                    info.path = file.getLocation().toOSString();

                    // Filter by targetFile if specified
                    if (targetFile != null && !targetFile.equals(file))
                    {
                        return Optional.empty();
                    }

                    if (file.exists())
                    {
                        info.markerHighlightedText = readContentFromFile(file, info.startLine, offset, length);
                    }
                }
                else if (targetFile != null)
                {
                    // If targetFile is specified but topObjectId is not a String, filter out
                    return Optional.empty();
                }
            }
            catch (Exception e)
            {
                //
            }
        }
        else if (targetFile != null)
        {
            // If targetFile is specified but positions are not available, filter out
            return Optional.empty();
        }

        // info.location;
        return Optional.ofNullable(info);
    }

    private String readContentFromFile(IFile file, int line, int offset, int length)
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
}
