/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.net.URI;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobChangeEvent;
import org.eclipse.core.runtime.jobs.JobChangeAdapter;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.operations.InstallOperation;
import org.eclipse.equinox.p2.operations.ProvisioningSession;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.artifact.IArtifactRepositoryManager;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.ServiceReference;

import com.e1c.edt.ai.IDefaultSettings;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ui.UINotificationService.UINotificationActionType;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class PluginUpdateService
    implements IPluginUpdateService
{
    @Inject
    private IUINotificationService notificationService;
    @Inject
    private ILog log;
    @Inject
    private IDispatcher dispatcher;
    @Inject
    ISettings settings;
    @Inject
    private IDefaultSettings defaultSettings;

    @Override
    public void checkForUpdates()
    {
        IProgressMonitor monitor = new NullProgressMonitor();
        try
        {
            var agent = getAgent();
            if (agent == null)
            {
                return;
            }

            var profiles = (IProfileRegistry)agent.getService(IProfileRegistry.SERVICE_NAME);
            var profile = profiles.getProfile(IProfileRegistry.SELF);
            if (profile == null)
            {
                log.trace("Update service is not available", () -> ""); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }

            var featureQuery = QueryUtil.createIUQuery(defaultSettings.getPluginFeature());
            var installedResult =
                profile.query(featureQuery, monitor);

            if (installedResult.isEmpty())
            {
                log.trace("The plugin is missing from the Installed Software", () -> ""); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }

            var currentVersion = installedResult.iterator().next().getVersion();
            var repositoryManager =
                (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
            var artifactManager = (IArtifactRepositoryManager)agent.getService(IArtifactRepositoryManager.SERVICE_NAME);

            var repositoryUri = new URI(settings.getUpdateUrl());
            if (!repositoryManager.contains(repositoryUri))
            {
                log.trace("Adding content repository...", () -> ""); //$NON-NLS-1$ //$NON-NLS-2$
                repositoryManager.addRepository(repositoryUri);
            }

            if (!artifactManager.contains(repositoryUri))
            {
                log.trace("Adding artifacts repository...", () -> ""); //$NON-NLS-1$ //$NON-NLS-2$
                artifactManager.addRepository(repositoryUri);
            }

            var repo = repositoryManager.loadRepository(repositoryUri, monitor);
            artifactManager.loadRepository(repositoryUri, monitor);

            var availableResult =
                repo.query(featureQuery, monitor);

            IInstallableUnit latestIU = null;
            for (var iu : availableResult)
            {
                if (latestIU == null || iu.getVersion().compareTo(latestIU.getVersion()) > 0)
                {
                    latestIU = iu;
                }
            }

            final var latest = latestIU;
            if (latestIU == null)
            {
                return;
            }

            var latestVersion = latestIU.getVersion();
            if (latestVersion.compareTo(currentVersion) > 0)
            {
                dispatcher.dispatchAsync(() -> notificationService.createNotificationWithAction(
                    PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.UpdateMessage, () -> {
                        installUpdate(agent, latest);
                    }, UINotificationActionType.UPDATE, UINotificationType.INFO, this.getClass()));

            }

        }
        catch (Exception error)
        {
            log.logError(error);
        }
    }

    @SuppressWarnings("nls")
    private void installUpdate(IProvisioningAgent agent, IInstallableUnit latestIU)
    {
        try
        {
            var session = new ProvisioningSession(agent);
            var installOp = new InstallOperation(session, List.of(latestIU));
            IStatus resolveStatus = installOp.resolveModal(new NullProgressMonitor());

            if (resolveStatus.getSeverity() == IStatus.ERROR)
            {
                log.trace("Failed to resolve dependencies", () -> "");
                return;
            }

            var job = installOp.getProvisioningJob(new NullProgressMonitor());
            job.addJobChangeListener(new JobChangeAdapter()
            {
                @Override
                public void done(IJobChangeEvent event)
                {
                    if (event.getResult().isOK())
                    {
                        log.trace("The update has been installed", () -> "");
                        dispatcher.dispatchAsync(() -> {
                            var shell = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell();
                            notificationService.createNotificationWithAction(shell, Messages.UpdateInstalled,
                                new Runnable()
                                {
                                    @Override
                                    public void run()
                                    {
                                        PlatformUI.getWorkbench().restart();
                                    }
                                }, UINotificationActionType.RELOAD, UINotificationType.INFO, this.getClass());
                        });
                    }
                    else
                    {
                        log.logError("Error during update installation");
                    }
                }
            });
            job.schedule();
        }
        catch (Exception e)
        {
            log.logError(e.getMessage());
        }
    }


    private IProvisioningAgent getAgent()
    {
        var context = BaseActivator.getDefault().getBundle().getBundleContext();
        ServiceReference<IProvisioningAgentProvider> agentProviderRef =
            context.getServiceReference(IProvisioningAgentProvider.class);
        var agentProvider = context.getService(agentProviderRef);
        IProvisioningAgent agent = null;
        try
        {
            agent = agentProvider.createAgent(null);
            return agent;
        }
        catch (ProvisionException error)
        {
            log.logError(error);
        }

        return null;
    }

}
