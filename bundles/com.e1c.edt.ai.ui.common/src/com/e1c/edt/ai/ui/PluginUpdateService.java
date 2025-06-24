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

import com.e1c.edt.ai.ILog;
import com.google.inject.Inject;

/**
 * @author Bogdan Sushkov
 *
 */
public class PluginUpdateService
    implements IPluginUpdateService
{
    private static final String REPOSITORY_URL = "https://code.1c.ai/plugin/"; //$NON-NLS-1$

    @Inject
    private IUINotificationService notificationService;
    @Inject
    private ILog log;
    @Inject
    private IDispatcher dispatcher;

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

            var installedResult =
                profile.query(QueryUtil.createIUQuery("com.e1c.edt.ai.feature.feature.group"), monitor); //$NON-NLS-1$

            if (installedResult.isEmpty())
            {
                log.trace("Плагин отсутствует в Установленном ПО", () -> ""); //$NON-NLS-1$ //$NON-NLS-2$
                return;
            }
            var currentVersion = installedResult.iterator().next().getVersion();

            var repositoryManager =
                (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
            var repositoryUri = new URI(REPOSITORY_URL);

            if (!repositoryManager.contains(repositoryUri))
            {
                log.trace("Репозиторий не найден, добавляем...", () -> ""); //$NON-NLS-1$ //$NON-NLS-2$
                repositoryManager.addRepository(repositoryUri);
            }

            var repo = repositoryManager.loadRepository(repositoryUri, monitor);

            var availableResult =
                repo.query(QueryUtil.createIUQuery("com.e1c.edt.ai.feature.feature.group"), monitor); //$NON-NLS-1$

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
                        Messages.UpdateLink, "https://code.1c.ai/easystart/", //$NON-NLS-1$
                        this.getClass()));
            }

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
        catch (ProvisionException e)
        {
            // empty stub
        }

        return null;
    }

}
