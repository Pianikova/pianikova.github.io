/**
 *
 */
package com.e1c.edt.ai;

import java.util.stream.Stream;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.assistent.model.MarkerInfo;

public interface IMarkersProvider
{
    Stream<MarkerInfo> getMarkers(IProject project, IFile file);
}
