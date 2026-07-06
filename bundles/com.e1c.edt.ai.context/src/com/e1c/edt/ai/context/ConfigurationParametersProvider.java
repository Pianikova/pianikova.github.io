/**
 *
 */
package com.e1c.edt.ai.context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.resources.IProject;

import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.core.platform.IExtensionProject;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com.e1c.edt.ai.IConfigurationParametersProvider;
import com.e1c.edt.ai.IProjectDetailsProvider;
import com.e1c.edt.ai.assistent.model.ConfigurationParameters;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ConfigurationParametersProvider
    implements IConfigurationParametersProvider, IProjectDetailsProvider
{
    /**
     *
     */
    private static final String DT_INF_PROJECT_PMF = "DT-INF/PROJECT.PMF"; //$NON-NLS-1$
    private static final String RUNTIME_VERSION = "Runtime-Version:"; //$NON-NLS-1$
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
        return getParameters(projectId.project);
    }

    @Override
    public void fill(IProject project, Map<String, Object> details)
    {
        getParameters(project).ifPresent(params -> {
            details.put("1C project details", params); //$NON-NLS-1$
        });
    }

    private Optional<ConfigurationParameters> getParameters(IProject project)
    {
        if (project == null)
        {
            return Optional.empty();
        }

        var v8Project = v8ProjectManager.getProject(project);
        if (v8Project != null)
        {
            var parameters = new ConfigurationParameters();
            getRuntimeVersion(project).ifPresent(runtimeVersion -> parameters.platformVersion = runtimeVersion);
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
                var configuration = configurationProject.getConfiguration();
                fillConfigData(parameters, configuration);
                // Only for regular configurations: extensions always have ADOPTED and stay editable
                if (configuration != null && configuration.getObjectBelonging() != null)
                {
                    parameters.objectBelonging = configuration.getObjectBelonging().getName();
                }
            }
            else
            {
                if (v8Project instanceof IExtensionProject)
                {
                    var extensionProject = (IExtensionProject)v8Project;
                    parameters.type = "Extension"; //$NON-NLS-1$
                    var parentProject = extensionProject.getParentProject();
                    if (parentProject != null)
                    {
                        parameters.parentProject = parentProject.getName();
                    }

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
        parameters.comment = config.getComment();
        parameters.briefInformation = config.getBriefInformation().map();
        var compatibilityMode = config.getCompatibilityMode();
        if (compatibilityMode != null)
        {
            parameters.compatibility = compatibilityMode.getLiteral();
        }
    }

    public Optional<String> getRuntimeVersion(IProject project)
    {
        var pmfFile = project.getFile(DT_INF_PROJECT_PMF);
        if (!pmfFile.exists())
        {
            return Optional.empty();
        }

        try (var inputStream = pmfFile.getContents();
            var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
        {

            String line;
            while ((line = reader.readLine()) != null)
            {
                if (line.startsWith(RUNTIME_VERSION))
                {
                    return Optional.of(line.substring(RUNTIME_VERSION.length()).trim());
                }
            }
        }
        catch (Exception e)
        {
            //
        }

        return Optional.empty();
    }
}
