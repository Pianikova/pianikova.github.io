/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.net.URI;
import java.util.Optional;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.core.IProvisioningAgentProvider;
import org.eclipse.equinox.p2.core.ProvisionException;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

/**
 * @author Bogdan Sushkov
 *
 */
public class PluginUpdateService
{
    private static final String REPOSITORY_URL = "https://code.1c.ai/plugin/"; //$NON-NLS-1$

    public static boolean isRepositoryConfigured()
    {
        try
        {
            BundleContext context = BaseActivator.getDefault().getBundle().getBundleContext();
            ServiceReference<IProvisioningAgentProvider> agentProviderRef =
                context.getServiceReference(IProvisioningAgentProvider.class);
            IProvisioningAgentProvider agentProvider = context.getService(agentProviderRef);
            IProvisioningAgent agent = agentProvider.createAgent(null);

            IMetadataRepositoryManager metadataManager =
                (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);

            URI repoUri = new URI(REPOSITORY_URL);
            return metadataManager.contains(repoUri);
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    public boolean checkForUpdates()
    {
        try
        {
            var agent = getAgent().get();

            IMetadataRepositoryManager metadataManager =
                (IMetadataRepositoryManager)agent.getService(IMetadataRepositoryManager.SERVICE_NAME);
            java.net.URI repoUri = new java.net.URI(REPOSITORY_URL);
            var repository = metadataManager.loadRepository(repoUri, new NullProgressMonitor());

            var query = QueryUtil.createLatestIUQuery();
            var result = repository.query(query, new NullProgressMonitor());
            for (IInstallableUnit iu : result)
            {
                // check for updates
                if (iu.getId().equals("com.e1c.edt.ai")) //$NON-NLS-1$
                {
                    return true;
                }
            }
            return false;
        }
        catch (Exception e)
        {
            e.printStackTrace();
            return false;
        }
    }

    private Optional<IProvisioningAgent> getAgent()
    {
        BundleContext context = BaseActivator.getDefault().getBundle().getBundleContext();
        ServiceReference<IProvisioningAgentProvider> agentProviderRef =
            context.getServiceReference(IProvisioningAgentProvider.class);
        IProvisioningAgentProvider agentProvider = context.getService(agentProviderRef);
        IProvisioningAgent agent = null;
        try
        {
            agent = agentProvider.createAgent(null);
            return Optional.of(agent);
        }
        catch (ProvisionException e)
        {
            // TODO Auto-generated catch block
        }

        return Optional.empty();
    }

}
