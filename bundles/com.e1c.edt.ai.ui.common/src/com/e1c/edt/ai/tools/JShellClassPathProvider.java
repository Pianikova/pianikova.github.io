/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;

import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import jdk.jshell.JShell;

/**
 * Provides classpath configuration for JShell sessions using standard OSGi mechanisms.
 */
@Singleton
class JShellClassPathProvider
	implements IJShellClassPathProvider
{
	private final ILog log;

	@Inject
	public JShellClassPathProvider(ILog log)
	{
        Preconditions.checkNotNull(log);
		this.log = log;
	}

	@Override
	public void addClassPathFor(JShell shell, Class<?> clazz)
	{
		try
		{
			var protectionDomain = clazz.getProtectionDomain();
			if (protectionDomain == null)
			{
				addBundleClassPathFor(shell, clazz);
				return;
			}

			var codeSource = protectionDomain.getCodeSource();
			if (codeSource == null || codeSource.getLocation() == null)
			{
				addBundleClassPathFor(shell, clazz);
				return;
			}

			Path path;
			try
			{
				path = Paths.get(codeSource.getLocation().toURI());
			}
			catch (URISyntaxException e)
			{
				// The code source URL may contain characters that are illegal in a URI
				// (e.g. spaces or parentheses in the installation directory such as
				// "1C_EDT (Lite) 2026.1 (2)"). Fall back to OSGi bundle resolution,
				// which uses the raw file path and tolerates such characters.
				addBundleClassPathFor(shell, clazz);
				return;
			}
			shell.addToClasspath(path.toString());
			addBinIfPresent(shell, path);
		}
		catch (Exception e)
		{
			log.logError("Failed to add classpath for " + clazz.getName() + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@Override
	public void addAllBundleClassPaths(JShell shell)
	{
		try
		{
			var context = FrameworkUtil.getBundle(JShellClassPathProvider.class).getBundleContext();
			if (context != null)
			{
				var bundles = context.getBundles();
				Set<String> addedBundles = new HashSet<>();
				for (var bundle : bundles)
				{
					addBundleClassPathWithDependencies(shell, bundle, addedBundles);
                }
			}
		}
		catch (Exception e)
		{
            log.logError("Failed to add all bundle classpaths: " + e.getMessage()); //$NON-NLS-1$
		}
	}

	private void addBundleClassPathFor(JShell shell, Class<?> clazz)
	{
		try
		{
			var bundle = FrameworkUtil.getBundle(clazz);
			if (bundle == null)
			{
				return;
			}

			var bundleFile = FileLocator.getBundleFile(bundle);
			if (bundleFile == null)
			{
				return;
			}

			shell.addToClasspath(bundleFile.getAbsolutePath());
			addBinIfPresent(shell, bundleFile.toPath());
		}
		catch (Exception e)
		{
			log.logError("Failed to add OSGi classpath for " + clazz.getName() + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void addBundleClassPathWithDependencies(JShell shell, Bundle bundle, Set<String> addedBundles)
	{
		if (bundle == null || addedBundles.contains(bundle.getSymbolicName()))
		{
			return;
		}

		try
		{
			if (bundle.getState() == Bundle.ACTIVE || bundle.getState() == Bundle.RESOLVED)
			{
				var bundleFile = FileLocator.getBundleFile(bundle);
				if (bundleFile != null)
				{
					shell.addToClasspath(bundleFile.getAbsolutePath());
					addBinIfPresent(shell, bundleFile.toPath());
					addedBundles.add(bundle.getSymbolicName());

					// Use standard OSGi BundleWiring API to get dependencies
					addBundleWiringDependencies(shell, bundle, addedBundles);
				}
			}
		}
		catch (Exception e)
		{
            log.logError("Failed to add bundle classpath for " + bundle.getSymbolicName() + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	private void addBundleWiringDependencies(JShell shell, Bundle bundle, Set<String> addedBundles)
	{
		try
		{
            var bundleWiring = bundle.adapt(BundleWiring.class);
			if (bundleWiring == null)
			{
				return;
			}

			// Get all required wires (dependencies) using standard OSGi API
            var requiredWires = bundleWiring.getRequiredWires("osgi.wiring.bundle"); //$NON-NLS-1$
			if (requiredWires != null)
			{
                for (var wire : requiredWires)
				{
					// Get the provider bundle from the wire
                    var providerWiring = wire.getProviderWiring();
					if (providerWiring != null)
					{
						Bundle providerBundle = providerWiring.getBundle();
						if (providerBundle != null)
						{
							addBundleClassPathWithDependencies(shell, providerBundle, addedBundles);
						}
					}
				}
			}
		}
		catch (Exception e)
		{
            log.logError("Failed to add wiring dependencies for " + bundle.getSymbolicName() + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

	@SuppressWarnings("nls")
    private void addBinIfPresent(JShell shell, Path root)
	{
		try
		{
			if (root == null)
			{
				return;
			}

            var candidate = root;

            if (Files.isRegularFile(root))
			{
				return;
			}

			if (root.endsWith("bin") || root.endsWith("target\\classes") || root.endsWith("target/classes"))
			{
				return;
			}

			candidate = root.resolve("bin");
			if (java.nio.file.Files.isDirectory(candidate))
			{
				shell.addToClasspath(candidate.toString());
			}

			candidate = root.resolve("target").resolve("classes");
			if (java.nio.file.Files.isDirectory(candidate))
			{
				shell.addToClasspath(candidate.toString());
			}
		}
		catch (Exception e)
		{
			log.logError("Failed to add bin classpath: " + e.getMessage()); //$NON-NLS-1$
		}
	}
}
