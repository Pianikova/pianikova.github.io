/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final int MAX_SESSIONS = 64;
    private static final int SESSION_EXPIRY_HOURS = 1;

    private final Cache<Integer, JShellSession> cache;
	private final ILog log;
	private final Set<IJShellBindingProvider> bindingProviders;
    private final IRestrictedTypesValidator restrictedTypesValidator;
    private final IJShellClassPathProvider classPathProvider;
    private final AtomicInteger sessionCounter = new AtomicInteger(0);

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

		// Cache with maximum sessions and expiration after access
		this.cache = CacheBuilder.newBuilder()
			.maximumSize(MAX_SESSIONS)
            .expireAfterAccess(SESSION_EXPIRY_HOURS, java.util.concurrent.TimeUnit.HOURS)
			.removalListener(notification -> {
				JShellSession session = (JShellSession)notification.getValue();
				if (session != null)
				{
					session.close();
				}
			})
			.build();
	}

    @SuppressWarnings("nls")
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

		// Session not found - should not happen for existing sessions
		throw new ToolException(
			"Session with ID " + sessionId + " not found. Sessions expire after " + SESSION_EXPIRY_HOURS
				+ " hour(s) of inactivity. Please create a new session first.",
			null, ToolErrorType.RETRYABLE);
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

        int sessionId = sessionCounter.incrementAndGet();
        var session = new JShellSession(sessionId, shell, outBuffer, errBuffer, restrictedTypesValidator, bindingProviders);

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
                var description = entry.getValue();
                var value = description.getValue();
                var explicitType = description.getExplicitType();
                var varName = entry.getKey();

                // Skip null bindings (e.g., documentation strings)
                if (value == null)
                {
                    continue;
                }

                var objectId = JShellObjectBridge.store(session.getSessionId(), value);
                var className = explicitType.getName();
                classPathProvider.addClassPathFor(shell, explicitType);
                var bindCode =
                    String.format("%s %s = (%s)com.e1c.edt.ai.tools.JShellObjectBridge.retrieve(%d, %d);",
                        className, varName, className, session.getSessionId(), objectId);
                var result = session.execute(bindCode);

                if (!result.compilationErrors.isEmpty() || !result.runtimeErrors.isEmpty())
                {
                    StringBuilder errorMessage = new StringBuilder();
                    errorMessage.append("JShell session creation failed, cannot bind: ```java\n");
                    errorMessage.append(bindCode);
                    errorMessage.append("\n```\n");
                    if (!result.compilationErrors.isEmpty())
                    {
                        errorMessage.append("\nCompilation errors:\n");
                        for (var error : result.compilationErrors)
                        {
                            errorMessage.append("  - ").append(error).append("\n");
                        }
                    }
                    if (!result.runtimeErrors.isEmpty())
                    {
                        errorMessage.append("\nRuntime errors:\n");
                        for (var error : result.runtimeErrors)
                        {
                            errorMessage.append("  - ").append(error).append("\n");
                        }
                    }
                    throw new ToolException(errorMessage.toString(), null, ToolErrorType.RETRYABLE);
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
