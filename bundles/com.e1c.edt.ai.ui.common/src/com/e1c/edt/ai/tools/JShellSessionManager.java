/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import com.e1c.edt.ai.ActionState;
import com.e1c.edt.ai.CancellationTokens;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.ToolErrorType;
import com.e1c.edt.ai.ToolException;
import com.e1c.edt.ai.assistent.IStateListener;
import com.e1c.edt.ai.ui.IDispatcher;
import com.e1c.edt.ai.ui.IInitializable;
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
public class JShellSessionManager
    implements IJShellSessionManager, IInitializable, IStateListener
{
    private static final int MAX_SESSIONS = 16;
    private static final int SESSION_EXPIRY_HOURS = 12;
    private static final int PREWARM_WAIT_TIMEOUT_MS = 30000;

    private final Cache<String, JShellSession> cache;
	private final ILog log;
	private final Set<IJShellBindingProvider> bindingProviders;
    private final IRestrictedTypesValidator restrictedTypesValidator;
    private final IJShellClassPathProvider classPathProvider;
    private final IDispatcher dispatcher;
    private final ISettings settings;
    private final IStateService stateService;
    private final AtomicReference<JShellSession> preWarmedSession = new AtomicReference<>();
    private final AtomicBoolean preWarmInFlight = new AtomicBoolean();
    private final Object sessionLock = new Object();

	@Inject
	public JShellSessionManager(ILog log, Set<IJShellBindingProvider> bindingProviders,
        IRestrictedTypesValidator restrictedTypesValidator, IJShellClassPathProvider classPathProvider,
        IDispatcher dispatcher, ISettings settings, IStateService stateService)
	{
		Preconditions.checkNotNull(log);
		Preconditions.checkNotNull(bindingProviders);
        Preconditions.checkNotNull(restrictedTypesValidator);
        Preconditions.checkNotNull(classPathProvider);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(stateService);

		this.log = log;
		this.bindingProviders = bindingProviders;
        this.restrictedTypesValidator = restrictedTypesValidator;
        this.classPathProvider = classPathProvider;
        this.dispatcher = dispatcher;
        this.settings = settings;
        this.stateService = stateService;

		this.cache = CacheBuilder.newBuilder()
			.maximumSize(MAX_SESSIONS)
            .expireAfterAccess(SESSION_EXPIRY_HOURS, TimeUnit.HOURS)
            .removalListener(notification -> {
                var session = (JShellSession)notification.getValue();
				if (session != null)
				{
					session.close();
				}
			})
			.build();
	}

    @Override
    public void initialize()
    {
        stateService.addListener(this);
        preWarmSessionAsync();
    }

    @Override
    public void onServiceStateChange(ServiceState serviceState)
    {
        preWarmSessionAsync();
    }

    @Override
    public void onActionStateChange(ActionState actionState)
    {
        // Do nothing
    }

    @SuppressWarnings("nls")
    @Override
    public IJShellSession getOrCreateSession(String sessionId)
	{
        if (sessionId == null || sessionId.isEmpty())
		{
            var preWarmed = preWarmedSession.getAndSet(null);
            if (preWarmed != null)
            {
                preWarmSessionAsync();
                return preWarmed;
            }

            synchronized (sessionLock)
            {
                preWarmed = preWarmedSession.get();
                if (preWarmed == null)
                {
                    try
                    {
                        sessionLock.wait(PREWARM_WAIT_TIMEOUT_MS);
                    }
                    catch (InterruptedException e)
                    {
                        Thread.currentThread().interrupt();
                    }

                    preWarmed = preWarmedSession.getAndSet(null);
                    if (preWarmed != null)
                    {
                        preWarmSessionAsync();
                        return preWarmed;
                    }
                }
            }

            var session = createSession();
            return session;
        }

        var session = cache.getIfPresent(sessionId);
		if (session != null)
		{
			return session;
		}

		throw new ToolException(
			"Session with ID " + sessionId + " not found. Sessions expire after " + SESSION_EXPIRY_HOURS
				+ " hour(s) of inactivity. Please create a new session first.",
			null, ToolErrorType.RETRYABLE);
	}

    @Override
    public IJShellSession getSession(String sessionId)
    {
        if (sessionId == null || sessionId.isEmpty())
        {
            return null;
        }

        return cache.getIfPresent(sessionId);
    }

	@SuppressWarnings("nls")
	private JShellSession createSession()
	{
        var snapshot = buildSessionSnapshot();
		var outBuffer = new ByteArrayOutputStream();
		var errBuffer = new ByteArrayOutputStream();

        var sessionClassLoader = new DelegatingClassLoader(JShellSessionManager.class.getClassLoader(),
            snapshot.classLoaders);
        var executionControlProvider = new JShellSharedExecutionControlProvider(sessionClassLoader);
        executionControlProvider.setOutputBuffers(outBuffer, errBuffer);

        var shell = JShell.builder()
            .executionEngine(executionControlProvider, Map.of())
			.build();

        String classpath = System.getProperty("java.class.path");
        shell.addToClasspath(classpath);
        classPathProvider.addClassPathFor(shell, JShellSessionManager.class);
        for (var significantClasses : snapshot.significantClassesByProvider.values())
        {
            for (var clazz : significantClasses)
            {
                classPathProvider.addClassPathFor(shell, clazz);
            }
        }
        classPathProvider.addAllBundleClassPaths(shell);

        String sessionId = UUID.randomUUID().toString();
        var sessionImports = snapshot.importsByProvider.values().stream()
            .flatMap(Collection::stream)
            .collect(java.util.stream.Collectors.toList());
        var session = new JShellSession(sessionId, shell, sessionClassLoader, outBuffer, errBuffer,
            restrictedTypesValidator, bindingProviders, sessionImports);

        // Set encoding for UTF-8 support
        session.execute("System.setProperty(\"file.encoding\", \"UTF-8\");");

        // Pre-import commonly used packages from providers
        for (var imports : snapshot.importsByProvider.values())
        {
            for (String imp : imports)
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
        for (var bindings : snapshot.bindingsByProvider.values())
		{
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

                var bindingType = explicitType != null ? explicitType : value.getClass();
                var objectId = JShellObjectBridge.store(session.getSessionId(), value);
                var className = bindingType.getName();
                classPathProvider.addClassPathFor(shell, bindingType);
                var bindCode =
                    String.format("%s %s = (%s)com.e1c.edt.ai.tools.JShellObjectBridge.retrieve(\"%s\", %d);",
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
                            errorMessage.append("  - Message: ").append(error.message).append("\n");
                            if (error.code != null)
                            {
                                errorMessage.append("    Error code: ").append(error.code).append("\n");
                            }
                            if (error.position >= 0)
                            {
                                errorMessage.append("    Position: ").append(error.position).append("\n");
                            }
                            if (error.startPosition >= 0)
                            {
                                errorMessage.append("    Start position: ").append(error.startPosition).append("\n");
                            }
                            if (error.endPosition >= 0)
                            {
                                errorMessage.append("    End position: ").append(error.endPosition).append("\n");
                            }
                            if (error.isResolutionError)
                            {
                                errorMessage.append("    Type: Resolution error\n");
                            }
                            if (error.isUnreachableError)
                            {
                                errorMessage.append("    Type: Unreachable code error\n");
                            }
                            if (error.isNotAStatementError)
                            {
                                errorMessage.append("    Type: Not a statement error\n");
                            }
                        }
                    }

                    if (!result.runtimeErrors.isEmpty())
                    {
                        errorMessage.append("\nRuntime errors:\n");
                        for (var error : result.runtimeErrors)
                        {
                            if (error.exceptionType != null)
                            {
                                errorMessage.append("  - Exception type: ").append(error.exceptionType).append("\n");
                            }
                            if (error.message != null)
                            {
                                errorMessage.append("    Message: ").append(error.message).append("\n");
                            }
                            if (error.stackTrace != null && !error.stackTrace.isBlank())
                            {
                                errorMessage.append("    Stack trace:\n").append(error.stackTrace).append("\n");
                            }
                        }
                    }

                    var error = errorMessage.toString();
                    log.logError(error);
                    throw new ToolException(error, null, ToolErrorType.RETRYABLE);
                }
			}
		}

		cache.put(session.getSessionId(), session);
		return session;
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

    private void preWarmSession()
    {
        try
        {
            synchronized (sessionLock)
            {
                // Check if we already have a pre-warmed session
                if (preWarmedSession.get() != null)
                {
                    return;
                }

                // Create new session
                var session = createSession();
                preWarmedSession.set(session);
                sessionLock.notifyAll();
            }
        }
        catch (Exception e)
        {
            log.logError("Failed to pre-warm JShell session: " + e.getMessage()); //$NON-NLS-1$
            synchronized (sessionLock)
            {
                sessionLock.notifyAll();
            }
        }
    }

    private void preWarmSessionAsync()
    {
        if (!settings.isEnabled() || preWarmedSession.get() != null || !preWarmInFlight.compareAndSet(false, true))
        {
            return;
        }

        dispatcher.createJob(Messages.JShellSessionPreWarming, context -> {
            try
            {
                preWarmSession();
            }
            finally
            {
                preWarmInFlight.set(false);
            }
        }, true, CancellationTokens.NONE).schedule();
    }

    private SessionSnapshot buildSessionSnapshot()
    {
        var snapshot = new SessionSnapshot();
        addClassLoader(snapshot.classLoaders, Thread.currentThread().getContextClassLoader());
        addClassLoader(snapshot.classLoaders, JShellSessionManager.class.getClassLoader());

        for (var provider : bindingProviders)
        {
            addClassLoader(snapshot.classLoaders, provider.getClass().getClassLoader());
            var bindings = provider.getBindings();
            if (bindings == null)
            {
                bindings = Map.of();
            }

            snapshot.bindingsByProvider.put(provider, bindings);
            for (var description : bindings.values())
            {
                if (description == null)
                {
                    continue;
                }

                addClassLoader(snapshot.classLoaders, description.getExplicitType());
                var value = description.getValue();
                if (value != null)
                {
                    addClassLoader(snapshot.classLoaders, value.getClass().getClassLoader());
                }
            }

            var significantClasses = provider.getSignificantClasses();
            if (significantClasses == null)
            {
                significantClasses = List.of();
            }

            snapshot.significantClassesByProvider.put(provider, significantClasses);
            for (var clazz : significantClasses)
            {
                addClassLoader(snapshot.classLoaders, clazz);
            }

            var imports = provider.getImports();
            snapshot.importsByProvider.put(provider, imports != null ? imports : List.of());
        }

        return snapshot;
    }

    private static void addClassLoader(Set<ClassLoader> classLoaders, Class<?> clazz)
    {
        if (clazz == null)
        {
            return;
        }

        addClassLoader(classLoaders, clazz.getClassLoader());
    }

    private static void addClassLoader(Set<ClassLoader> classLoaders, ClassLoader classLoader)
    {
        if (classLoader != null)
        {
            classLoaders.add(classLoader);
        }
    }

    private static final class SessionSnapshot
    {
        private final Map<IJShellBindingProvider, Map<String, JShellBindingDescription>> bindingsByProvider =
            new LinkedHashMap<>();
        private final Map<IJShellBindingProvider, Collection<Class<?>>> significantClassesByProvider =
            new LinkedHashMap<>();
        private final Map<IJShellBindingProvider, Collection<String>> importsByProvider = new LinkedHashMap<>();
        private final Set<ClassLoader> classLoaders = new LinkedHashSet<>();
    }

    /**
     * Resolves classes through multiple OSGi-aware classloaders before JShell falls back to URL classpath loading.
     */
    private static final class DelegatingClassLoader
        extends ClassLoader
    {
        private final List<ClassLoader> delegates;

        private DelegatingClassLoader(ClassLoader parent, Set<ClassLoader> classLoaders)
        {
            super(parent);
            this.delegates = new ArrayList<>();
            for (var classLoader : classLoaders)
            {
                if (classLoader == null || classLoader == parent)
                {
                    continue;
                }
                this.delegates.add(classLoader);
            }
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException
        {
            synchronized (getClassLoadingLock(name))
            {
                var alreadyLoaded = findLoadedClass(name);
                if (alreadyLoaded != null)
                {
                    return alreadyLoaded;
                }

                try
                {
                    return super.loadClass(name, resolve);
                }
                catch (ClassNotFoundException e)
                {
                    for (var delegate : delegates)
                    {
                        try
                        {
                            return delegate.loadClass(name);
                        }
                        catch (ClassNotFoundException ignored)
                        {
                            // Try the next classloader.
                        }
                    }
                    throw e;
                }
            }
        }
    }
}
