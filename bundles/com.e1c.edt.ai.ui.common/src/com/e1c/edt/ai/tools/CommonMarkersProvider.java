/**
 *
 */
package com.e1c.edt.ai.tools;

import java.util.ArrayList;
import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.BadLocationException;

import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IMarkersProvider;
import com.e1c.edt.ai.assistent.model.MarkerInfo;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class CommonMarkersProvider
    implements IMarkersProvider
{
    private final IContentSourceProvider contentSourceProvider;

    @Inject
    public CommonMarkersProvider(IContentSourceProvider contentSourceProvider)
    {
        Preconditions.checkNotNull(contentSourceProvider);
        this.contentSourceProvider = contentSourceProvider;
    }

    @SuppressWarnings("nls")
    @Override
    public Stream<MarkerInfo> getMarkers(IProject project, IFile file)
    {
        var allMarkers = new ArrayList<MarkerInfo>();
        try
        {
            var markers = file == null ? project.findMarkers(null, true, IResource.DEPTH_INFINITE)
                : file.findMarkers(null, true, IResource.DEPTH_INFINITE);
            // Process each marker
            for (var marker : markers)
            {
                // Determine marker type using enum
                var markerType = MarkerType.fromTypeId(marker.getType());
                if (markerType == null)
                {
                    continue; // Skip unknown types
                }

                var resource = marker.getResource();
                var location = resource.getLocation();

                var markerInfo = new MarkerInfo();
                markerInfo.path = location != null ? location.toFile().getAbsolutePath() : "";
                markerInfo.startLine = marker.getAttribute(IMarker.LINE_NUMBER, -1);
                markerInfo.message = marker.getAttribute(IMarker.MESSAGE, "");
                markerInfo.type = markerType.getDisplayName();

                // Set common and type-specific attributes
                setMarkerAttributes(project, marker, markerInfo, markerType);
                allMarkers.add(markerInfo);
            }
        }
        catch (CoreException e)
        {
            //
        }

        return allMarkers.stream();
    }

    private void setMarkerAttributes(IProject project, IMarker marker, MarkerInfo markerInfo, MarkerType markerType)
        throws CoreException
    {
        // Common attributes for all marker types
        markerInfo.location = marker.getAttribute(IMarker.LOCATION, null);

        // Get char positions
        Integer charStart = null;
        Integer charEnd = null;
        var charStartObj = marker.getAttribute(IMarker.CHAR_START);
        if (charStartObj instanceof Integer)
        {
            charStart = (Integer)charStartObj;
        }

        var charEndObj = marker.getAttribute(IMarker.CHAR_END);
        if (charEndObj instanceof Integer)
        {
            charEnd = (Integer)charEndObj;
        }

        // Read target content if positions are available
        if (charStart != null && charEnd != null && charEnd > charStart)
        {
            try
            {
                var file = (IFile)marker.getResource();
                if (file.exists())
                {
                    markerInfo.markerHighlightedText = readContentFromFile(file, charStart, charEnd - charStart);
                }
            }
            catch (Exception e)
            {
                // Ignore errors and leave markerHighlightedText empty
            }
        }

        // Type-specific attributes
        switch (markerType)
        {
        case BOOKMARK:
            var doneBookmark = marker.getAttribute(IMarker.DONE);
            if (doneBookmark instanceof Boolean)
            {
                markerInfo.done = (Boolean)doneBookmark;
            }
            var sourceId = marker.getAttribute(IMarker.SOURCE_ID);
            if (sourceId instanceof String)
            {
                markerInfo.sourceId = (String)sourceId;
            }
            break;

        case TASK:
            var doneTask = marker.getAttribute(IMarker.DONE);
            if (doneTask instanceof Boolean)
            {
                markerInfo.done = (Boolean)doneTask;
            }
            var priorityObj = marker.getAttribute(IMarker.PRIORITY);
            if (priorityObj instanceof Integer)
            {
                int priority = (Integer)priorityObj;
                markerInfo.priority = convertPriorityToString(priority);
            }
            break;

        case PROBLEM:
        case AI_MARKER:
            var severityObj = marker.getAttribute(IMarker.SEVERITY);
            if (severityObj instanceof Integer)
            {
                int severity = (Integer)severityObj;
                markerInfo.severity = convertSeverityToString(severity);
            }
            var priorityProblem = marker.getAttribute(IMarker.PRIORITY);
            if (priorityProblem instanceof Integer)
            {
                int priority = (Integer)priorityProblem;
                markerInfo.priority = convertPriorityToString(priority);
            }
            break;

        default:
            // No additional attributes for other types
            break;
        }
    }

    private String readContentFromFile(IFile file, int charStart, int length) throws BadLocationException
    {
        var optionalDocument = contentSourceProvider.getFileDocument(file);
        if (optionalDocument.isEmpty())
        {
            return null;
        }

        var document = optionalDocument.get();
        return document.getDocument().get(charStart, length);
    }

    private String convertSeverityToString(int severity)
    {
        switch (severity)
        {
        case IMarker.SEVERITY_ERROR:
            return MarkerInfo.SEVERITY_ERROR;
        case IMarker.SEVERITY_WARNING:
            return MarkerInfo.SEVERITY_WARNING;
        default:
            return MarkerInfo.SEVERITY_INFO;
        }
    }

    private String convertPriorityToString(int priority)
    {
        switch (priority)
        {
        case IMarker.PRIORITY_HIGH:
            return MarkerInfo.PRIORITY_HIGH;
        case IMarker.PRIORITY_LOW:
            return MarkerInfo.PRIORITY_LOW;
        default:
            return MarkerInfo.PRIORITY_NORMAL;
        }
    }
}
