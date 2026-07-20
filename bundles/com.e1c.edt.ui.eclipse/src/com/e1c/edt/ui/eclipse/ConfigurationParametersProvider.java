/**
 *
 */
package com.e1c.edt.ui.eclipse;

import java.util.Optional;

import org.eclipse.core.resources.IProject;

import com.e1c.edt.ai.IConfigurationParametersProvider;
import com.e1c.edt.ai.IProjectParametersProvider;
import com.e1c.edt.ai.assistent.model.ConfigurationParameters;
import com.e1c.edt.ai.assistent.model.ProjectParameters;

public class ConfigurationParametersProvider
    implements IConfigurationParametersProvider, IProjectParametersProvider
{
    @Override
    public Optional<ConfigurationParameters> getParameters(IProject project)
    {
        return Optional.empty();
    }

    @Override
    public Optional<ProjectParameters> getProjectParameters(IProject project)
    {
        if (project == null)
        {
            return Optional.empty();
        }

        var parameters = new ProjectParameters();
        parameters.uuid = ProjectParameters.limitUuid(project.getName());
        parameters.configurationName = project.getName();
        return Optional.of(parameters);
    }
}
