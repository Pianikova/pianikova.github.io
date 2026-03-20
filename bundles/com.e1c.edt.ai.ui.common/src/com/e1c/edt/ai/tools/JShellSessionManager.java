/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.FrameworkUtil;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.google.common.base.Preconditions;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import jdk.jshell.JShell;

/**
 * Cache for JShell REPL sessions.
 */
@Singleton
class JShellSessionManager
	implements IJShellSessionManager
{
	private final Cache<String, JShellSession> cache;
	private final ILog log;
	private final Set<IJShellBindingProvider> bindingProviders;
    private final IRestrictedTypesValidator restrictedTypesValidator;

	@Inject
	public JShellSessionManager(ILog log, Set<IJShellBindingProvider> bindingProviders,
        IRestrictedTypesValidator restrictedTypesValidator)
	{
		Preconditions.checkNotNull(log);
		Preconditions.checkNotNull(bindingProviders);
        Preconditions.checkNotNull(restrictedTypesValidator);

		this.log = log;
		this.bindingProviders = bindingProviders;
        this.restrictedTypesValidator = restrictedTypesValidator;

		// Cache with maximum of 64 sessions and 30 minutes expiration after access
		this.cache = CacheBuilder.newBuilder()
			.maximumSize(64)
            .expireAfterAccess(1, java.util.concurrent.TimeUnit.HOURS)
			.removalListener(notification -> {
				JShellSession session = (JShellSession)notification.getValue();
				if (session != null)
				{
					session.close();
				}
			})
			.build();
	}

	@Override
	public IJShellSession getOrCreateSession(String sessionId)
	{
		if (sessionId == null || sessionId.isBlank())
		{
			// Create new session
			return createSession();
		}

		// Get existing session
        var session = cache.getIfPresent(sessionId);
		if (session != null)
		{
			return session;
		}

		// Session not found, create new one
		return createSession();
	}

    @Override
    public IJShellSession getSession(String sessionId)
    {
        if (sessionId == null || sessionId.isBlank())
        {
            return null;
        }
        return cache.getIfPresent(sessionId);
    }

	@SuppressWarnings("nls")
	private JShellSession createSession()
	{
		var outBuffer = new ByteArrayOutputStream();
		var errBuffer = new ByteArrayOutputStream();

        var executionControlProvider = new SharedExecutionControlProvider(JShellSessionManager.class.getClassLoader());
        executionControlProvider.setOutputBuffers(outBuffer, errBuffer);

        var shell = JShell.builder()
            .executionEngine(executionControlProvider, Map.of())
			.build();

        String classpath = System.getProperty("java.class.path");
        shell.addToClasspath(classpath);
        addClassPathFor(shell, JShellSessionManager.class);
        for (var provider : bindingProviders)
        {
            for (var clazz : provider.getSignificantClasses())
            {
                addClassPathFor(shell, clazz);
            }
        }
        addAllBundleClassPaths(shell);

        var session = new JShellSession(shell, outBuffer, errBuffer, restrictedTypesValidator);

        // Pre-import commonly used packages from providers
        for (var provider : bindingProviders)
        {
            for (String imp : provider.getImports())
            {
                try
                {
                    var result = session.execute(imp);
                    if (!result.compilationErrors.isEmpty())
                    {
                        log.logError("Failed to import: " + imp);
                    }
                }
                catch (Exception e)
                {
                    log.logError("Failed to import package: " + e.getMessage());
                }
            }
        }

		// Store bindings in registry
        for (var provider : bindingProviders)
		{
			var bindings = provider.getBindings();
			for (var entry : bindings.entrySet())
			{
				try
				{
                    int objectId = JShellObjectBridge.store(entry.getValue());
                    var value = entry.getValue();
                    var type = value.getClass();
                    var className = type.getName();
                    var varName = entry.getKey();
                    addClassPathFor(shell, type);
                    var bindCode = String.format("%s %s = (%s)com.e1c.edt.ai.tools.JShellObjectBridge.retrieve(%d);",
                        className, varName, className, objectId);
                    var result = session.execute(bindCode);
                    if (!result.compilationErrors.isEmpty() || !result.runtimeErrors.isEmpty())
                    {
                        throw new ToolException(
                            "JShell session creation failed, сannot bind: ```java\n" + bindCode + "\n```",
                            ToolErrorType.RETRYABLE);
                    }
				}
				catch (Exception e)
				{
					log.logError("Failed to bind " + entry.getKey() + ": " + e.getMessage());
				}
			}
		}

		cache.put(session.getSessionId(), session);
		return session;
	}

    private void addClassPathFor(JShell shell, Class<?> clazz)
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

            var location = codeSource.getLocation().toURI();
            var path = Paths.get(location);
            shell.addToClasspath(path.toString());
            addBinIfPresent(shell, path);
        }
        catch (Exception e)
        {
            log.logError("Failed to add classpath for " + clazz.getName() + ": " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$
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

    @SuppressWarnings({ "nls" })
    private void addBinIfPresent(JShell shell, java.nio.file.Path root)
    {
        try
        {
            if (root == null)
            {
                return;
            }

            java.nio.file.Path candidate = root;

            if (java.nio.file.Files.isRegularFile(root))
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

    @SuppressWarnings("nls")
    private void addAllBundleClassPaths(JShell shell)
    {
        try
        {
            var context = FrameworkUtil.getBundle(JShellSessionManager.class).getBundleContext();
            if (context != null)
            {
                var bundles = context.getBundles();
                java.util.Set<String> addedBundles = new java.util.HashSet<>();
                for (var bundle : bundles)
                {
                    addBundleClassPathWithDependencies(shell, bundle, addedBundles);
                }
                log.logError("Added " + addedBundles.size() + " bundles to JShell classpath");
            }
        }
        catch (Exception e)
        {
            log.logError("Failed to add all bundle classpaths: " + e.getMessage());
        }
    }

    private void addBundleClassPathWithDependencies(JShell shell, org.osgi.framework.Bundle bundle,
        java.util.Set<String> addedBundles)
    {
        if (bundle == null || addedBundles.contains(bundle.getSymbolicName()))
        {
            return;
        }

        try
        {
            if (bundle.getState() == org.osgi.framework.Bundle.ACTIVE || bundle.getState() == org.osgi.framework.Bundle.RESOLVED)
            {
                var bundleFile = FileLocator.getBundleFile(bundle);
                if (bundleFile != null)
                {
                    shell.addToClasspath(bundleFile.getAbsolutePath());
                    addBinIfPresent(shell, bundleFile.toPath());
                    addedBundles.add(bundle.getSymbolicName());

                    // Add required bundles from Require-Bundle header
                    addRequiredBundles(shell, bundle, addedBundles);
                }
            }
        }
        catch (Exception e)
        {
            log.logError("Failed to add bundle classpath for " + bundle.getSymbolicName() + ": " + e.getMessage());
        }
    }

    @SuppressWarnings("nls")
    private void addRequiredBundles(JShell shell, org.osgi.framework.Bundle bundle,
        java.util.Set<String> addedBundles)
    {
        try
        {
            var headers = bundle.getHeaders();
            var requireBundle = headers.get(org.osgi.framework.Constants.REQUIRE_BUNDLE);
            if (requireBundle != null)
            {
                String[] requiredBundleNames = requireBundle.toString().split(",");
                var context = FrameworkUtil.getBundle(JShellSessionManager.class).getBundleContext();
                if (context != null)
                {
                    var bundles = context.getBundles();
                    for (String requiredName : requiredBundleNames)
                    {
                        String bundleName = requiredName.trim();
                        if (bundleName.contains(";"))
                        {
                            bundleName = bundleName.substring(0, bundleName.indexOf(";"));
                        }
                        for (var reqBundle : bundles)
                        {
                            if (reqBundle.getSymbolicName().equals(bundleName))
                            {
                                addBundleClassPathWithDependencies(shell, reqBundle, addedBundles);
                                break;
                            }
                        }
                    }
                }
            }
        }
        catch (Exception e)
        {
            log.logError("Failed to add required bundles for " + bundle.getSymbolicName() + ": " + e.getMessage());
        }
    }

	@Override
	public void invalidateSession(String sessionId)
	{
		cache.invalidate(sessionId);
	}

	@Override
	public void invalidateAll()
	{
		cache.invalidateAll();
    }
}
