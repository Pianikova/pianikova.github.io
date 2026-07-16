/*
 * Copyright (c) 2026, ООО 1С-Софт
 */
package com.e1c.edt.ai.ui.quickfix;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;

import com._1c.g5.v8.dt.validation.marker.Marker;

/** Selected leaf markers from the EDT Configuration Problems view. */
final class ConfigurationProblemSelection
{
    private final IProject project;
    private final List<Marker> markers;

    private ConfigurationProblemSelection(IProject project, List<Marker> markers)
    {
        this.project = project;
        this.markers = Collections.unmodifiableList(markers);
    }

    static Optional<ConfigurationProblemSelection> from(ISelection selection)
    {
        if (!(selection instanceof IStructuredSelection) || selection.isEmpty())
        {
            return Optional.empty();
        }

        IProject project = null;
        List<Marker> markers = new ArrayList<>();
        for (Object element : ((IStructuredSelection)selection).toList())
        {
            Marker marker = Adapters.adapt(element, Marker.class);
            if (marker == null || marker.getProject() == null)
            {
                return Optional.empty();
            }
            if (project == null)
            {
                project = marker.getProject();
            }
            else if (!project.equals(marker.getProject()))
            {
                return Optional.empty();
            }
            markers.add(marker);
        }
        return markers.isEmpty() ? Optional.empty()
            : Optional.of(new ConfigurationProblemSelection(project, markers));
    }

    IProject getProject()
    {
        return project;
    }

    List<Marker> getMarkers()
    {
        return markers;
    }
}
