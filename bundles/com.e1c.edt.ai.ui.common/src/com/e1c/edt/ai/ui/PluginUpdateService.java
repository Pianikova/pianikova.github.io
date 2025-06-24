/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.net.URI;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.engine.IProfileRegistry;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.ServiceReference;

import com.e1c.edt.ai.IDefaultSettings;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettingsProvider;
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
    ISettingsProvider settingsProvider;
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
                log.trace("The  plugin is missing from the Installed Software", () -> ""); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }

            var currentVersion = installedResult.iterator().next().getVersion();
            var repositoryManager =
                (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
            var repositoryUri = new URI(settingsProvider.getSettings().getLlmParameters().updateUrl);
            if (!repositoryManager.contains(repositoryUri))
            {
                log.trace("Repository not found, adding...", () -> ""); //$NON-NLS-1$ //$NON-NLS-2$
                repositoryManager.addRepository(repositoryUri);
            }

            var repo = repositoryManager.loadRepository(repositoryUri, monitor);

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

            if (latestIU == null)
            {
                return;
            }

            var latestVersion = latestIU.getVersion();
            if (latestVersion.compareTo(currentVersion) > 0)
            {
                dispatcher.dispatchAsync(() -> notificationService.createNotification(
                        PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), Messages.UpdateMessage,
                    Messages.UpdateLink, defaultSettings.getHomePage() + "easystart/", //$NON-NLS-1$
                        this.getClass()));
            }

        }
        catch (Exception error)
        {
            log.logError(error);
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
