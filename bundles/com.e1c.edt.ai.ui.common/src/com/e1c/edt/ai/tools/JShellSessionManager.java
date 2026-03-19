/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.e1c.edt.ai.ILog;
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
	private final Map<String, Object> bindingRegistry = new ConcurrentHashMap<>();

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
		JShellSessionCacheHolder.setInstance(this);

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
			.out(new PrintStream(outBuffer))
			.err(new PrintStream(errBuffer))
			.build();

		// Store bindings in registry
		for (var provider : bindingProviders)
		{
			var bindings = provider.getBindings();
			for (var entry : bindings.entrySet())
			{
				try
				{
					bindingRegistry.put(entry.getKey(), entry.getValue());
					var className = entry.getValue().getClass().getName();
					shell.eval(String.format("import %s;", className));
					shell.eval(String.format("%s %s = (%s) com.e1c.edt.ai.tools.JShellSessionCache.getBinding(\"%s\");",
						className, entry.getKey(), className, entry.getKey()));
				}
				catch (Exception e)
				{
					log.logError("Failed to bind " + entry.getKey() + ": " + e.getMessage());
				}
			}
		}

		var session = new JShellSession(shell, outBuffer, errBuffer, restrictedTypesValidator);
		cache.put(session.getSessionId(), session);
		return session;
	}

	public static Object getBinding(String name)
	{
		return JShellSessionCacheHolder.getBinding(name);
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

	private static class JShellSessionCacheHolder
	{
		private static JShellSessionManager instance;

		static void setInstance(JShellSessionManager cache)
		{
			instance = cache;
		}

		static Object getBinding(String name)
		{
			return instance != null ? instance.bindingRegistry.get(name) : null;
		}
	}
}
