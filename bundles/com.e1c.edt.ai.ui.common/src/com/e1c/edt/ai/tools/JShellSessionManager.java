/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Set;

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
    private final Cache<Integer, JShellSession> cache;
	private final ILog log;
	private final Set<IJShellBindingProvider> bindingProviders;
    private final IRestrictedTypesValidator restrictedTypesValidator;
    private final IJShellClassPathProvider classPathProvider;

	@Inject
	public JShellSessionManager(ILog log, Set<IJShellBindingProvider> bindingProviders,
        IRestrictedTypesValidator restrictedTypesValidator, IJShellClassPathProvider classPathProvider)
	{
		Preconditions.checkNotNull(log);
		Preconditions.checkNotNull(bindingProviders);
        Preconditions.checkNotNull(restrictedTypesValidator);
        Preconditions.checkNotNull(classPathProvider);

		this.log = log;
		this.bindingProviders = bindingProviders;
        this.restrictedTypesValidator = restrictedTypesValidator;
        this.classPathProvider = classPathProvider;

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
    public IJShellSession getOrCreateSession(int sessionId)
	{
        if (sessionId == 0)
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
    public IJShellSession getSession(int sessionId)
    {
        if (sessionId == 0)
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

        var executionControlProvider = new JShellSharedExecutionControlProvider(JShellSessionManager.class.getClassLoader());
        executionControlProvider.setOutputBuffers(outBuffer, errBuffer);

        var shell = JShell.builder()
            .executionEngine(executionControlProvider, Map.of())
			.build();

        String classpath = System.getProperty("java.class.path");
        shell.addToClasspath(classpath);
        classPathProvider.addClassPathFor(shell, JShellSessionManager.class);
        for (var provider : bindingProviders)
        {
            for (var clazz : provider.getSignificantClasses())
            {
                classPathProvider.addClassPathFor(shell, clazz);
            }
        }
        classPathProvider.addAllBundleClassPaths(shell);

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
                    var objectId = JShellObjectBridge.store(session.getSessionId(), entry.getValue());
                    var value = entry.getValue();
                    var type = value.getClass();
                    var className = type.getName();
                    var varName = entry.getKey();
                    classPathProvider.addClassPathFor(shell, type);
                    var bindCode =
                        String.format("%s %s = (%s)com.e1c.edt.ai.tools.JShellObjectBridge.retrieve(%d, %d);",
                            className, varName, className, session.getSessionId(), objectId);
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

	@Override
    public void invalidateSession(int sessionId)
	{
		cache.invalidate(sessionId);
	}

	@Override
	public void invalidateAll()
	{
		cache.invalidateAll();
    }
}
