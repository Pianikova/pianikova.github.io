/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.PrintStream;
import java.net.URI;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.runtime.FileLocator;
import org.osgi.framework.Bundle;
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

        var shell = JShell.builder()
            .executionEngine(new SharedExecutionControlProvider(JShellSessionManager.class.getClassLoader()), Map.of())
			.out(new PrintStream(outBuffer))
			.err(new PrintStream(errBuffer))
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

        var session = new JShellSession(shell, outBuffer, errBuffer, restrictedTypesValidator);

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
                return;
            }
            var codeSource = protectionDomain.getCodeSource();
            if (codeSource == null || codeSource.getLocation() == null)
            {
                addBundleClassPathFor(shell, clazz);
                return;
            }
            URI location = codeSource.getLocation().toURI();
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
            Bundle bundle = FrameworkUtil.getBundle(clazz);
            if (bundle == null)
            {
                return;
            }
            File bundleFile = FileLocator.getBundleFile(bundle);
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
