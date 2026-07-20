/**
 *
 */
package com.e1c.edt.ai.context;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IProject;

import com._1c.g5.v8.dt.core.platform.IConfigurationProject;
import com._1c.g5.v8.dt.core.platform.IDtProject;
import com._1c.g5.v8.dt.core.platform.IExtensionProject;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.metadata.mdclass.Configuration;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.v8.dt.platform.version.Version;
import com.e1c.edt.ai.IConfigurationParametersProvider;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectDetailsProvider;
import com.e1c.edt.ai.IProjectParametersProvider;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.model.ConfigurationParameters;
import com.e1c.edt.ai.assistent.model.ProjectParameters;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ConfigurationParametersProvider
    implements IConfigurationParametersProvider, IProjectDetailsProvider, IProjectParametersProvider
{
    /**
     *
     */
    private static final String DT_INF_PROJECT_PMF = "DT-INF/PROJECT.PMF"; //$NON-NLS-1$
    private static final String RUNTIME_VERSION = "Runtime-Version:"; //$NON-NLS-1$
    private static final String TRACE_TOPIC = "ProjectParameters"; //$NON-NLS-1$
    private final IV8ProjectManager v8ProjectManager;
    private final IRuntimeVersionSupport runtimeVersionSupport;
    private final ILog log;

    @Inject
    public ConfigurationParametersProvider(IV8ProjectManager v8ProjectManager,
        IRuntimeVersionSupport runtimeVersionSupport, ILog log)
    {
        Preconditions.checkNotNull(v8ProjectManager);
        Preconditions.checkNotNull(runtimeVersionSupport);
        Preconditions.checkNotNull(log);
        this.v8ProjectManager = v8ProjectManager;
        this.runtimeVersionSupport = runtimeVersionSupport;
        this.log = log;
    }

    @Override
    public void fill(IProject project, Map<String, Object> details)
    {
        getParameters(project).ifPresent(params -> {
            details.put("1C project details", params); //$NON-NLS-1$
        });
    }

    @Override
    public Optional<ConfigurationParameters> getParameters(IProject project)
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
            parameters.availablePlatformVersions = runtimeVersionSupport.getSupportedVersions()
                .stream()
                .map(Version::toString)
                .collect(Collectors.toList());
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

    @SuppressWarnings("nls")
    @Override
    public Optional<ProjectParameters> getProjectParameters(IProject project)
    {
        if (project == null)
        {
            log.trace(TracingSources.API_CALLS, TRACE_TOPIC, () -> "getProjectParameters: project is null (global session)");
            return Optional.empty();
        }

        var v8Project = v8ProjectManager.getProject(project);
        if (v8Project == null)
        {
            // Основная причина отсутствия project_parameters: workspace-проект ещё не зарегистрирован как
            // V8-проект (гонка на старте EDT, проект ещё открывается) либо это не 1С-проект.
            log.trace(TracingSources.API_CALLS, TRACE_TOPIC,
                () -> "getProjectParameters: v8ProjectManager.getProject(" + project.getName()
                    + ") == null (accessible=" + project.isAccessible() + ", open=" + project.isOpen()
                    + ") -> project_parameters НЕ будет отправлен");
            return Optional.empty();
        }

        IDtProject dtProject = null;
        Configuration configuration = null;
        if (v8Project instanceof IConfigurationProject)
        {
            var configurationProject = (IConfigurationProject)v8Project;
            dtProject = configurationProject.getDtProject();
            configuration = configurationProject.getConfiguration();
        }
        else if (v8Project instanceof IExtensionProject)
        {
            var extensionProject = (IExtensionProject)v8Project;
            dtProject = extensionProject.getDtProject();
            configuration = extensionProject.getConfiguration();
        }
        else
        {
            var v8ProjectType = v8Project.getClass().getSimpleName();
            log.trace(TracingSources.API_CALLS, TRACE_TOPIC, () -> "getProjectParameters: v8Project для " + project.getName()
                + " не Configuration/Extension, а " + v8ProjectType + " -> ожидаем");
            return Optional.empty();
        }

        // uuid проекта — это UUID корневого объекта Configuration (стабильный, глобально уникальный),
        // а НЕ dtProject.getId() (для workspace-проектов он равен имени проекта). Он доступен только после
        // загрузки метаданных проекта в V8-модель, поэтому пока конфигурация не загружена — возвращаем empty,
        // и воркфлоу трекинга ждёт (гейт по isPresent()).
        if (configuration == null || configuration.getUuid() == null)
        {
            var dtProjectName = dtProject != null ? dtProject.getName() : project.getName();
            var reason = configuration == null ? "configuration=null" : "configuration.uuid=null";
            log.trace(TracingSources.API_CALLS, TRACE_TOPIC,
                () -> "getProjectParameters: конфигурация ещё не загружена в V8-модель для " + dtProjectName
                    + " (" + reason + ") -> ожидаем загрузку");
            return Optional.empty();
        }

        var parameters = new ProjectParameters();
        parameters.uuid = ProjectParameters.limitUuid(configuration.getUuid().toString());
        if (dtProject != null)
        {
            parameters.configurationName = dtProject.getName();
        }

        getRuntimeVersion(project).ifPresent(runtimeVersion -> parameters.platformVersion = runtimeVersion);
        parameters.configurationVersion = configuration.getVersion();
        var compatibilityMode = configuration.getCompatibilityMode();
        if (compatibilityMode != null)
        {
            parameters.compatibilityMode = compatibilityMode.getLiteral();
        }

        log.trace(TracingSources.API_CALLS, TRACE_TOPIC, () -> "getProjectParameters: OK для " + project.getName()
            + " uuid=" + parameters.uuid + ", configuration_name=" + parameters.configurationName
            + ", platform_version=" + parameters.platformVersion + ", configuration_version="
            + parameters.configurationVersion);
        return Optional.of(parameters);
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
            // Ignored
        }

        return Optional.empty();
    }
}
