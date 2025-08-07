/**
 *
 */
package com.e1c.edt.ai.context;

import java.util.Optional;

import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.core.platform.IExtensionProject;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com.e1c.edt.ai.IConfigurationParametersProvider;
import com.e1c.edt.ai.assistent.model.ConfigurationParameters;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ConfigurationParametersProvider
    implements IConfigurationParametersProvider
{
    private final IV8ProjectManager v8ProjectManager;

    @Inject
    public ConfigurationParametersProvider(IV8ProjectManager v8ProjectManager)
    {
        Preconditions.checkNotNull(v8ProjectManager);
        this.v8ProjectManager = v8ProjectManager;
    }

    @Override
    public Optional<ConfigurationParameters> getParameters(ProjectId projectId)
    {
        var v8Project = v8ProjectManager.getProject(projectId.project);
        if (v8Project != null)
        {
            var parameters = new ConfigurationParameters();
            var scriptVariant = v8Project.getScriptVariant();
            if (scriptVariant != null)
            {
                parameters.scriptLanguage = scriptVariant.getName();
            }

            if (v8Project instanceof IConfigurationProject)
            {
                var configurationProject = (IConfigurationProject)v8Project;
                parameters.type = "Configuration"; //$NON-NLS-1$
                fillProjectData(parameters, configurationProject.getDtProject());
                fillConfigData(parameters, configurationProject.getConfiguration());
            }
            else
            {
                if (v8Project instanceof IExtensionProject)
                {
                    var extensionProject = (IExtensionProject)v8Project;
                    parameters.type = "Extension"; //$NON-NLS-1$
                    fillProjectData(parameters, extensionProject.getDtProject());
                    fillConfigData(parameters, extensionProject.getConfiguration());
                }
            }

            return Optional.of(parameters);
        }

        return Optional.empty();
    }

    private void fillProjectData(ConfigurationParameters parameters, IDtProject dtProject)
    {
        if (dtProject == null)
        {
            return;
        }

        parameters.name = dtProject.getName();
    }

    private void fillConfigData(ConfigurationParameters parameters, Configuration config)
    {
        if (config == null)
        {
            return;
        }

        parameters.vendor = config.getVendor();
        parameters.version = config.getVersion();
    }
}
