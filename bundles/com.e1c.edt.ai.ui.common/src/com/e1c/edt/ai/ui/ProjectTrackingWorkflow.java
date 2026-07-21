/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension4;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.Fields;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.IHashTools;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectParametersProvider;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.TracingSources;
import com.e1c.edt.ai.assistent.ISessionService;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class ProjectTrackingWorkflow
    implements IProjectTrackingWorkflow
{
    private final static Duration ExtraLongDelay = Duration.ofSeconds(30);
    private final static Duration LongDelay = Duration.ofSeconds(3);
    private final static Duration ShortDelay = Duration.ofMillis(10);
    // Added/changed/removed files are now discovered live via resource deltas (ProjectTrackingDeltaVisitor),
    // so a full project re-scan is only a reconcile safety net for events that were missed and runs rarely.
    private final static int HashCyclesBetweenScans = 20;
    // How many fast (LongDelay) polls to wait for V8 registration before backing off to ExtraLongDelay.
    private final static int ReadinessFastRetries = 10;
    private final ILog log;
    private final Provider<IStatistics> statisticsProvider;
    private final IHashTools hashTools;
    private final IClock clock;
    private final IGlobalContextSync globalContextSync;
    private final ISettings settings;
    private final IFileScaner fileScaner;
    private final ISessionService sessionService;
    private final IProjectParametersProvider projectParametersProvider;
    private final IGlobalContextStateStore stateStore;
    private final HashSet<ProjectFile> filesToSync = new HashSet<>();
    private final ConcurrentHashMap<String, ProjectFile> filesToHash = new ConcurrentHashMap<>();
    private IProject project;
    private volatile boolean resetRequested;
    private ProjectTrackingWorkflowState nextState = ProjectTrackingWorkflowState.INIT;
    private int iterationCount = Integer.MAX_VALUE;
    private int readinessWaitCycles = 0;
    // Whether the persisted per-project state has been loaded into filesToHash yet (once per initialize()).
    private boolean seeded;
    // Set when filesToHash content changed in a way that must be persisted (rehash, prune, seed cleanup).
    private volatile boolean stateDirty;

    @Inject
    public ProjectTrackingWorkflow(ILog log, Provider<IStatistics> statisticsProvider, IHashTools hashTools,
        IClock clock, IGlobalContextSync globalContextSync, ISettings settings,
        IFileScaner fileScaner, ISessionService sessionService,
        IProjectParametersProvider projectParametersProvider, IGlobalContextStateStore stateStore)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(statisticsProvider);
        Preconditions.checkNotNull(hashTools);
        Preconditions.checkNotNull(clock);
        Preconditions.checkNotNull(globalContextSync);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(fileScaner);
        Preconditions.checkNotNull(sessionService);
        Preconditions.checkNotNull(projectParametersProvider);
        Preconditions.checkNotNull(stateStore);
        this.log = log;
        this.statisticsProvider = statisticsProvider;
        this.hashTools = hashTools;
        this.clock = clock;
        this.globalContextSync = globalContextSync;
        this.settings = settings;
        this.fileScaner = fileScaner;
        this.sessionService = sessionService;
        this.projectParametersProvider = projectParametersProvider;
        this.stateStore = stateStore;
    }

    @Override
    public ProjectTrackingWorkflow initialize(IProject project)
    {
        Preconditions.checkNotNull(project);
        this.project = project;
        iterationCount = Integer.MAX_VALUE;
        readinessWaitCycles = 0;
        seeded = false;
        stateDirty = false;
        return this;
    }

    @Override
    public IProject getProject()
    {
        return this.project;
    }

    @Override
    public Duration nextState(IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
    {
        if (!settings.isEnabled() || !project.isAccessible())
        {
            return ExtraLongDelay;
        }

        // Apply a pending reset on the tracking thread (requestReset() may be called from any thread).
        if (resetRequested)
        {
            resetRequested = false;
            reset();
        }

        Result result = null;
        try
        {
            switch (nextState)
            {
            case INIT:
                result = init(progressMonitor, cancellationToken);
                break;

            case SCAN:
                result = scan(progressMonitor, cancellationToken);
                break;

            case HASH:
                result = hash(1000, progressMonitor, cancellationToken);
                break;

            case SYNC:
                result = sync(1000, progressMonitor, cancellationToken);
                break;
            }
        }
        catch (Exception error)
        {
            var failedState = nextState;
            log.trace(TracingSources.API_CALLS, "Sync error", //$NON-NLS-1$
                () -> failedState + " (" + project.getName() + "): " + error); //$NON-NLS-1$ //$NON-NLS-2$
            return LongDelay;
        }

        if (result != null)
        {
            nextState = result.nextState;
            return result.delay;
        }

        return LongDelay;
    }

    @Override
    public void requestReset()
    {
        resetRequested = true;
    }

    private void reset()
    {
        filesToSync.clear();
        for (var file : filesToHash.values())
        {
            file.update(clock.now(), null, 1);
        }
    }

    /**
     * Loads the persisted per-project state and seeds {@link #filesToHash}. Seeded entries carry the previous
     * session's local timestamp and MD5 hash, so hash() skips them while their file is unchanged. Files that no
     * longer exist on disk are dropped (they fall out of the state on the next persist).
     */
    private void seedFromPersistedState()
    {
        Map<String, GlobalContextFileState> saved;
        try
        {
            saved = stateStore.load(project);
        }
        catch (Exception error)
        {
            log.logError(error);
            return;
        }

        for (var entry : saved.entrySet())
        {
            var portablePath = entry.getKey();
            var state = entry.getValue();
            if (state == null || state.hash == null)
            {
                continue;
            }

            var path = Path.fromPortableString(portablePath);
            if (path.segmentCount() <= 1)
            {
                continue;
            }

            var file = project.getFile(path.removeFirstSegments(1));
            if (file == null || !file.exists())
            {
                // Deleted while EDT was closed: don't seed. It won't be re-added, so it self-prunes from the state.
                stateDirty = true;
                continue;
            }

            filesToHash.computeIfAbsent(portablePath, key -> {
                var seededFile = new ProjectFile(new AIContext(project, key, (IDocument)null), key, file, clock.now());
                seededFile.update(clock.now(), state.hash, state.time);
                return seededFile;
            });
        }
    }

    /**
     * Persists a snapshot of the current sync state. Only quiescent, on-disk, already-hashed files are stored: open
     * editors are skipped so an unsaved buffer's hash is never paired with the disk file's timestamp. Because the
     * snapshot is rebuilt from the live map, deleted files drop out automatically.
     */
    private void persistState()
    {
        var snapshot = new HashMap<String, GlobalContextFileState>();
        for (var file : filesToHash.values())
        {
            if (file.aiCtx.getDocument() != null)
            {
                continue;
            }

            var hash = file.getHash();
            // Persist the stored stamp (not the live getLocalTimeStamp()): update() always sets hash + stamp
            // together, so the stored pair is always self-consistent even if persist runs before hash() has
            // re-validated a freshly seeded entry. For a document-less entry this stamp is a local timestamp.
            var time = file.getModificationStamp();
            if (hash == null || time <= 0)
            {
                continue;
            }

            var state = new GlobalContextFileState();
            state.time = time;
            state.hash = hash;
            snapshot.put(file.path, state);
        }

        stateStore.save(project, snapshot);
    }

    @Override
    public void track(AIContext aiCtx)
    {
        log.trace(TracingSources.SYNC, "Track", () -> aiCtx.toString()); //$NON-NLS-1$
        var path = aiCtx.getPath();
        var projectPath = project.getProjectRelativePath().append(path);
        if (projectPath.segmentCount() <= 1)
        {
            return;
        }

        var fileOnDisk = project.getFile(projectPath.removeFirstSegments(1));
        if (fileOnDisk == null)
        {
            return;
        }

        var fileToTrack = new ProjectFile(aiCtx, path, fileOnDisk, LocalDateTime.MIN);
        fileToTrack.update(clock.now(), null, 1);
        filesToHash.put(path, fileToTrack);
    }

    private Result init(IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
    {
        // Flush the persisted state at a quiescent point (debounced by the dirty flag) so restarts stay incremental.
        if (stateDirty)
        {
            stateDirty = false;
            try
            {
                persistState();
            }
            catch (Exception error)
            {
                log.logError(error);
            }
        }

        if (iterationCount < HashCyclesBetweenScans)
        {
            iterationCount++;
            return new Result(ProjectTrackingWorkflowState.HASH, LongDelay);
        }
        else
        {
            iterationCount = 0;
            return new Result(ProjectTrackingWorkflowState.SCAN, ShortDelay);
        }
    }

    @SuppressWarnings("nls")
    private Result scan(IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
        throws CoreException
    {
        progressMonitor.subTask(Messages.CodeCompletionBackgroundScanSubtaskName);

        // Don't warm the session until the project is registered as a V8 project. At EDT startup the workflow
        // runs before IV8ProjectManager has registered the (already accessible/open) project, so its parameters
        // are not yet resolvable. Creating the session now would cache a session without project_parameters for
        // the whole EDT run (ResponseCache keeps successful sessions), binding memory to the wrong/absent id.
        // We wait by returning a delay: this runs on the tracking Job (never the UI thread) and the tracker loop
        // re-checks cancellation before every nextState(), so the wait is non-blocking and cancellable.
        if (projectParametersProvider.getProjectParameters(project).isEmpty())
        {
            readinessWaitCycles++;
            var cycles = readinessWaitCycles;
            log.trace(TracingSources.API_CALLS, "ProjectReadiness", //$NON-NLS-1$
                () -> project.getName() + ": project not registered as a V8 project yet, deferring session warm-up" //$NON-NLS-1$
                    + " (cycle " + cycles + ")"); //$NON-NLS-1$ //$NON-NLS-2$
            // Poll quickly for the first few cycles (registration usually completes within seconds), then back
            // off. Missing the warm-up is not fatal: the session is created lazily on first completion/chat,
            // by which time the project is registered.
            var delay = cycles > ReadinessFastRetries ? ExtraLongDelay : LongDelay;
            return new Result(ProjectTrackingWorkflowState.SCAN, delay);
        }
        readinessWaitCycles = 0;

        if (!settings.sendGlobalContext(project))
        {
            // The server decides per project (via the session it returns) whether global context is synced,
            // and exposes that through sendGlobalContext(). Establish the session asynchronously so those
            // parameters get applied, then keep re-checking the gate. getSessionAsync() is cached, so this is
            // a cheap no-op once the session exists. Without it the workflow would never sync until some other
            // path (e.g. code completion) happened to create the session — global context sync is expected to
            // begin right at EDT startup.
            sessionService.getSessionAsync(project);
            return new Result(ProjectTrackingWorkflowState.SCAN, LongDelay);
        }

        // Seed filesToHash from the persisted state before the fresh scan, so hash() can skip files whose local
        // timestamp still matches (no MD5, no sync). Done once per initialize(), on the tracking Job (never the UI
        // thread). computeIfAbsent below preserves these seeded entries (with their hash + timestamp) instead of
        // overwriting them with fresh stamp=-1 ProjectFiles.
        if (!seeded)
        {
            seeded = true;
            seedFromPersistedState();
        }

        List<IFile> files = fileScaner.scan(project);
        var now = clock.now();
        for (var file : files)
        {
            // Skip files that don't exist on disk. getLocation() is null for resources without a local
            // path (e.g. linked/virtual), so guard against it before touching the filesystem.
            var location = file.getLocation();
            if (location == null || !location.toFile().exists())
            {
                continue;
            }

            var path = file.getFullPath().makeRelative().toPortableString();
            filesToHash.computeIfAbsent(path,
                key -> new ProjectFile(new AIContext(project, key, (IDocument)null), key, file, now));

            if (cancellationToken.isCanceled())
            {
                return new Result(ProjectTrackingWorkflowState.SCAN, ShortDelay);
            }
        }

        // Drop tracked entries for files that no longer exist (e.g. deleted) and are not backed by an open
        // document, so filesToHash does not retain stale ProjectFiles indefinitely. IResource.exists() is an
        // in-memory workspace check, not disk I/O.
        if (filesToHash.values()
            .removeIf(tracked -> tracked.aiCtx.getDocument() == null && !tracked.file.exists()))
        {
            stateDirty = true;
        }

        var newFilesToHashCount = 0;
        for (var file : filesToHash.values())
        {
            if (file.getModificationStamp() <= 0 && file.getHash() == null)
            {
                newFilesToHashCount++;
            }
        }

        var newFilesToHashCountVal = newFilesToHashCount;
        log.trace(TracingSources.SYNC, "Scaned", () -> {
            var message = new StringBuilder();
            message.append("Project: ");
            message.append(project.getName());

            message.append(System.lineSeparator());
            message.append("Files: ");
            message.append(files.size());

            message.append(System.lineSeparator());
            message.append("New files to hash: ");
            message.append(newFilesToHashCountVal);
            return message.toString();
        });

        if (newFilesToHashCount > 0)
        {
            return new Result(ProjectTrackingWorkflowState.HASH, ShortDelay);
        }

        return new Result(ProjectTrackingWorkflowState.INIT, LongDelay);
    }

    @SuppressWarnings("nls")
    private Result hash(int maxFiles, IProgressMonitor progressMonitor,
        ICancellationToken cancellationToken)
    {
        var filesToSyncSize = filesToSync.size();
        maxFiles = maxFiles - filesToSyncSize;
        if (maxFiles <= 0)
        {
            return new Result(ProjectTrackingWorkflowState.INIT, Duration.ofMillis(100));
        }

        var fileToSyncCount = 0;
        var delay = LongDelay;
        var now = clock.now();
        var maxFilesFinal = maxFiles;
        var hashingFiles =
            filesToHash.values()
                .stream()
                .filter(i -> !cancellationToken.isCanceled())
                .filter(file -> file.getAge(now).compareTo(delay) >= 0)
                .sorted(ProjectFile.COMPARATOR)
                .limit(maxFilesFinal)
                .collect(Collectors.toList());

        var hashed = 0;
        progressMonitor.beginTask(Messages.CodeCompletionBackgroundHashSubtaskName, hashingFiles.size());
        for (var file : hashingFiles)
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            try
            {
                var fileOnDisk = file.file;
                if (fileOnDisk != null && file.aiCtx.getDocument() == null
                    && (!fileOnDisk.exists() || !fileOnDisk.isAccessible()))
                {
                    if (filesToHash.remove(file.path) == null)
                    {
                        continue;
                    }
                }

                var prevModificationStamp = file.getModificationStamp();
                long newModificationStamp = -1;
                var prevHash = file.getHash();
                String newHash;
                var document = file.aiCtx.getDocument();
                var isAccessible = true;
                if (!file.aiCtx.isDisposed() && document != null && document instanceof IDocumentExtension4)
                {
                    var docExtension = (IDocumentExtension4)document;
                    newModificationStamp = docExtension.getModificationStamp();
                    if (file.getModificationStamp() == newModificationStamp)
                    {
                        file.update(now, prevHash, newModificationStamp);
                        continue;
                    }
                }
                else
                {
                    document = null;
                    isAccessible = file.file.isAccessible();
                    // Use the filesystem timestamp (not getModificationStamp()): it is comparable across EDT
                    // restarts, which is what lets a seeded entry from a previous session match an unchanged file
                    // and skip re-hashing. getModificationStamp() is an in-session workspace counter and is not
                    // guaranteed to survive a restart.
                    newModificationStamp = file.file.getLocalTimeStamp();
                    if (isAccessible && file.getModificationStamp() == newModificationStamp)
                    {
                        file.update(now, prevHash, newModificationStamp);
                        continue;
                    }
                }

                newHash = isAccessible
                    ? hashTools.hashOf(document, file.file).map(hash -> hashTools.format(hash, true)).orElse(null)
                    : null;
                file.update(now, newHash, newModificationStamp);
                hashed++;
                // Content (or its timestamp) changed for this file: the persisted state needs a refresh.
                stateDirty = true;
                if (newHash != null && newHash.equals(prevHash))
                {
                    continue;
                }

                fileToSyncCount++;
                if (filesToSync.add(file))
                {
                    var modificationStamp = newModificationStamp;
                    var accessible = isAccessible;
                    log.trace(TracingSources.SYNC, "Sync required", () -> {
                        var message = new StringBuilder();
                        message.append("Project: ");
                        message.append(project.getName());

                        message.append(System.lineSeparator());
                        message.append("File: ");
                        message.append(file.path);

                        message.append(System.lineSeparator());
                        message.append("Is accessible: ");
                        message.append(accessible);

                        message.append(System.lineSeparator());
                        message.append("Prev timestamp: ");
                        message.append(prevModificationStamp);
                        if (prevHash != null)
                        {
                            message.append(System.lineSeparator());
                            message.append("Prev hash: ");
                            message.append(prevHash);
                        }

                        message.append(System.lineSeparator());
                        message.append("New timestamp: ");
                        message.append(modificationStamp);

                        message.append(System.lineSeparator());
                        message.append("New hash: ");
                        message.append(newHash == null ? "empty" : newHash);
                        return message.toString();
                    });
                }
            }
            catch (Exception error)
            {
                log.logError(error);
            }
            finally
            {
                progressMonitor.worked(1);
            }
        }

        if (hashed > 0)
        {
            var hashedVal = hashed;
            log.trace(TracingSources.SYNC, "Hashed", () -> {
                var message = new StringBuilder();
                message.append("Project: ");
                message.append(project.getName());

                message.append(System.lineSeparator());
                message.append("Hashed: ");
                message.append(hashedVal);
                message.append(" files");

                return message.toString();
            });
        }

        if (fileToSyncCount > 0)
        {
            return new Result(ProjectTrackingWorkflowState.SYNC, ShortDelay);
        }

        return new Result(ProjectTrackingWorkflowState.INIT, LongDelay);
    }

    @SuppressWarnings("nls")
    private Result sync(int maxFiles, IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
    {
        if (maxFiles <= 0)
        {
            return new Result(ProjectTrackingWorkflowState.HASH, ShortDelay);
        }

        var maxFilesFinal = maxFiles;
        var filesToProcess =
            filesToSync.stream()
                .filter(file -> !cancellationToken.isCanceled())
                .sorted(ProjectFile.COMPARATOR)
                .limit(maxFilesFinal)
                .collect(Collectors.toList());

        var features = new ArrayList<CompletableFuture<Boolean>>();
        var filesUpdates = new ArrayList<GlobalContextUpdate>();
        for (var file : filesToProcess)
        {
            if (cancellationToken.isCanceled())
            {
                return new Result(ProjectTrackingWorkflowState.SYNC, ShortDelay);
            }

            filesToSync.remove(file);
            var update = new GlobalContextUpdate();
            var ext = file.file.getFileExtension();
            if (ext == null)
            {
                continue;
            }

            switch (file.file.getFileExtension())
            {
            case "bsl":
                update.field = Fields.LOCAL_FUNCTIONS;
                break;

            case "mdo":
                update.field = Fields.META;
                break;

            case "form":
                update.field = Fields.FORM;
                break;

            default:
                continue;
            }

            update.path = file.path;
            update.hash = file.getHash();
            if (file.aiCtx.getDocument() != null)
            {
                var documentUpdates = new ArrayList<GlobalContextUpdate>();
                documentUpdates.add(update);
                features.add(
                    globalContextSync.syncUpdates(file.aiCtx, documentUpdates, 5, statisticsProvider.get(),
                        cancellationToken));
                continue;
            }

            filesUpdates.add(update);
        }

        if (!filesUpdates.isEmpty())
        {
            features.add(
                globalContextSync.syncUpdates(new AIContext(project, "", (IDocument)null), filesUpdates, 5, //$NON-NLS-1$
                    statisticsProvider.get(), cancellationToken));
        }

        CompletableFuture.allOf(features.toArray(new CompletableFuture[features.size()])).join();
        if (!filesToSync.isEmpty())
        {
            return new Result(ProjectTrackingWorkflowState.SYNC, ShortDelay);
        }

        return new Result(ProjectTrackingWorkflowState.HASH, ShortDelay);
    }

    private static class Result
    {
        public final ProjectTrackingWorkflowState nextState;
        public final Duration delay;

        public Result(ProjectTrackingWorkflowState nextState, Duration delay)
        {
            Preconditions.checkNotNull(nextState);
            Preconditions.checkNotNull(delay);
            this.nextState = nextState;
            this.delay = delay;
        }
    }
}
