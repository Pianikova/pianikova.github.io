/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.io.ByteArrayInputStream;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.AIContextKind;
import com.e1c.edt.ai.Fields;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.IHashTools;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.IUISettings;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class ProjectTrackingWorkflow
    implements IProjectTrackingWorkflow
{
    private final static int MaxConcurrentSyncs = 1;
    private final static Duration ExtraLongDelay = Duration.ofSeconds(3);
    private final static Duration LongDelay = Duration.ofSeconds(1);
    private final static Duration ShortDelay = Duration.ofMillis(10);
    private final static ProjectFileComparator ProjectFileComparator = new ProjectFileComparator();
    private final ILog log;
    private final Provider<IStatistics> statisticsProvider;
    private final IHashTools hashTools;
    private final IClock clock;
    private final IProjectIdProvider projectIdProvider;
    private final IGlobalContextSync globalContextSync;
    private final IUISettings settings;
    private final IFileScaner fileScaner;
    private final HashMap<String, ProjectFile> filesToHash = new HashMap<>();
    private final HashSet<ProjectFile> filesToSync = new HashSet<>();
    private final HashMap<String, FileToTrack> filesToTrack = new HashMap<>();
    private final CharBuffer buffer = CharBuffer.allocate(1024);
    private final Object lockObject = new Object();
    private GlobalContextState state;
    private IProject project;
    private ProjectId projectId;
    private ProjectTrackingWorkflowState nextState = ProjectTrackingWorkflowState.INIT;

    @Inject
    public ProjectTrackingWorkflow(ILog log, Provider<IStatistics> statisticsProvider, IHashTools hashTools,
        IClock clock, IProjectIdProvider projectIdProvider,
        IGlobalContextSync globalContextSync, IUISettings settings, IFileScaner fileScaner)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(statisticsProvider);
        Preconditions.checkNotNull(hashTools);
        Preconditions.checkNotNull(clock);
        Preconditions.checkNotNull(projectIdProvider);
        Preconditions.checkNotNull(globalContextSync);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(fileScaner);
        this.log = log;
        this.statisticsProvider = statisticsProvider;
        this.hashTools = hashTools;
        this.clock = clock;
        this.projectIdProvider = projectIdProvider;
        this.globalContextSync = globalContextSync;
        this.settings = settings;
        this.fileScaner = fileScaner;
    }

    @Override
    public ProjectTrackingWorkflow initialize(IProject project, GlobalContextState state)
    {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(state);
        synchronized (lockObject)
        {
            this.project = project;
            this.state = state;
            projectId = projectIdProvider.getProjectId(project);
        }

        return this;
    }

    @Override
    public String getId()
    {
        return projectId.path;
    }

    @Override
    public Duration nextState(IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
    {
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
                result = sync(300, progressMonitor, cancellationToken);
                break;
            }
        }
        catch (Exception error)
        {
            log.logError(error);
            return ExtraLongDelay;
        }

        if (result != null)
        {
            nextState = result.nextState;
            return result.delay;
        }

        return ExtraLongDelay;
    }

    @Override
    public void track(AIContext aiCtx)
    {
        synchronized (lockObject)
        {
            log.debug("Track", () -> aiCtx.toString()); //$NON-NLS-1$
            filesToTrack.compute(aiCtx.getPath(), (key, prev) -> new FileToTrack(aiCtx));
        }
    }

    @Override
    public void saveState(GlobalContextState state)
    {
        synchronized (lockObject)
        {
            for (var filesToHash : filesToHash.values())
            {
                if (filesToHash.isEmpty())
                {
                    continue;
                }

                state.files.put(filesToHash.path, filesToHash.getState());
            }
        }
    }

    private Result init(IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
    {
        return new Result(ProjectTrackingWorkflowState.SCAN, ShortDelay);
    }

    @SuppressWarnings("nls")
    private Result scan(IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
        throws CoreException
    {
        progressMonitor.subTask(Messages.CodeCompletionBackgroundScanSubtaskName);
        final var hasFileToHash = new Boolean[1];
        hasFileToHash[0] = false;

        var sendGlobalContext = settings.sendGlobalContext();
        var filesToRemove = new ArrayList<ProjectFile>();
        synchronized (lockObject)
        {
            for (var file : filesToHash.values())
            {
                if (!file.file.exists() || (!sendGlobalContext && file.aiCtx.getKind() == AIContextKind.Common))
                {
                    filesToRemove.add(file);
                }
            }

            for (var file : filesToRemove)
            {
                filesToHash.remove(file.path);
            }
        }

        if (sendGlobalContext)
        {
            List<IFile> files = fileScaner.scan(project);
            var now = clock.now();
            synchronized (lockObject)
            {
                for (var file : files)
                {
                    var path = file.getFullPath().makeRelative().toPortableString();
                    if (!filesToHash.containsKey(path))
                    {
                        var document =
                            Optional.ofNullable(filesToTrack.get(path)).map(i -> i.aiCtx.getDocument()).orElse(null);
                        hasFileToHash[0] = true;
                        var aiCtx = new AIContext(projectId, AIContextKind.Common, 0, "", 0, path, "", 0, document);
                        filesToHash.put(path, initProjectFile(new ProjectFile(aiCtx, path, file, now)));
                    }
                }
            }
        }

        synchronized (lockObject)
        {
            if (filesToTrack.size() > 0)
            {
                hasFileToHash[0] = true;
            }
        }

        return new Result(ProjectTrackingWorkflowState.HASH, hasFileToHash[0] ? ShortDelay : ExtraLongDelay);
    }

    @SuppressWarnings("nls")
    private Result hash(int maxFiles, IProgressMonitor progressMonitor,
        ICancellationToken cancellationToken)
    {
        maxFiles = maxFiles - filesToSync.size();
        if (maxFiles <= 0)
        {
            return new Result(ProjectTrackingWorkflowState.SYNC, Duration.ofMillis(100));
        }

        var now = clock.now();
        synchronized (lockObject)
        {
            var processedFiles = new ArrayList<String>();
            for (var fileToTrack : filesToTrack.entrySet())
            {
                var path = fileToTrack.getKey();
                ProjectFile fileToHash;
                fileToHash = filesToHash.computeIfAbsent(path,
                    key -> initProjectFile(new ProjectFile(fileToTrack.getValue().aiCtx, path,
                        project.getFile(project.getProjectRelativePath().append(path).removeFirstSegments(1)), now)));

                processedFiles.add(path);
                if (!fileToHash.file.isAccessible())
                {
                    filesToHash.remove(path);
                    continue;
                }

                fileToHash.updateTime = now;
                fileToHash.aiCtx = fileToTrack.getValue().aiCtx;
            }

            for (var processedFile : processedFiles)
            {
                filesToTrack.remove(processedFile);
            }
        }

        final var hasFileToSync = new Boolean[1];
        hasFileToSync[0] = false;
        var delay = Duration.ofSeconds(3);
        var filesToProcess =
            filesToHash.values()
                .stream()
                .filter(i -> i.hasState() || Duration.between(i.updateTime, now).compareTo(delay) >= 0)
                .sorted(ProjectFileComparator)
                .limit(maxFiles)
                .collect(Collectors.toList());

        progressMonitor.beginTask(Messages.CodeCompletionBackgroundHashSubtaskName, filesToProcess.size());
        for (var file : filesToProcess)
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            try
            {
                var prevHash = file.hash;
                file.updateTime = now;
                if (file.aiCtx.getKind() == AIContextKind.Common)
                {
                    if (!file.file.isAccessible())
                    {
                        continue;
                    }

                    var modificationStamp = file.file.getModificationStamp();
                    if (file.modificationStamp != modificationStamp)
                    {
                        log.debug("Sync required", () -> {
                            var message = new StringBuilder();
                            message.append("File: ");
                            message.append(file.path);

                            message.append(System.lineSeparator());
                            message.append("Prev timestamp: ");
                            message.append(file.modificationStamp);

                            message.append(System.lineSeparator());
                            message.append("Cur timestamp: ");
                            message.append(modificationStamp);
                            return message.toString();
                        });

                        file.modificationStamp = modificationStamp;
                        file.hash = hashTools.format(hashTools.compute(file.file, buffer), true);
                    }
                    else
                    {
                        if (file.hasState())
                        {
                            if (filesToSync.add(file))
                            {
                                hasFileToSync[0] = true;
                            }

                            continue;
                        }
                    }
                }
                else
                {
                    try (var inputStream =
                        new ByteArrayInputStream(file.aiCtx.getSource().getBytes(StandardCharsets.UTF_8));)
                    {
                        file.hash =
                            hashTools.format(hashTools.compute(inputStream, StandardCharsets.UTF_8, buffer), true);
                    }
                }

                if (!file.hasState() && file.hash.equals(prevHash))
                {
                    continue;
                }

                if (prevHash != null)
                {
                    log.debug("Sync required", () -> {
                        var message = new StringBuilder();
                        message.append("File: ");
                        message.append(file.path);

                        message.append(System.lineSeparator());
                        message.append("Prev hash: ");
                        message.append(prevHash);

                        message.append(System.lineSeparator());
                        message.append("Cur hash: ");
                        message.append(file.hash);
                        return message.toString();
                    });
                }

                if (filesToSync.add(file))
                {
                    hasFileToSync[0] = true;
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

        return new Result(hasFileToSync[0] ? ProjectTrackingWorkflowState.SYNC : ProjectTrackingWorkflowState.HASH,
            ShortDelay);
    }

    private Result sync(int maxFiles, IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
    {
        var filesToProcess =
            filesToSync.stream().sorted(ProjectFileComparator).limit(maxFiles).collect(Collectors.toList());

        var filesUpdates = new ArrayList<GlobalContextUpdate>();
        for (var file : filesToProcess)
        {
            var update = new GlobalContextUpdate();
            update.field = Fields.LOCAL_FUNCTIONS;
            update.path = file.path;
            update.hash = file.hash;
            filesUpdates.add(update);
        }

        Set<String> unknowFilePaths;
        try
        {
            unknowFilePaths =
                globalContextSync.sync(projectId, filesUpdates, statisticsProvider.get(), cancellationToken)
                    .get()
                    .map(i -> i.unknownValues)
                    .orElseGet(() -> Collections.emptyList())
                    .stream()
                    .map(i -> i.path)
                    .collect(Collectors.toSet());
        }
        catch (InterruptedException | ExecutionException error)
        {
            log.logError(error);
            return new Result(ProjectTrackingWorkflowState.HASH, LongDelay);
        }

        progressMonitor.beginTask(Messages.CodeCompletionBackgroundSyncSubtaskName, filesToProcess.size());
        var futures = new ArrayList<CompletableFuture<Boolean>>();
        for (var file : filesToProcess)
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            progressMonitor.worked(1);
            file.updateState();

            if (!unknowFilePaths.contains(file.path))
            {
                synchronized (lockObject)
                {
                    filesToSync.remove(file);
                }

                continue;
            }

            var statistics = statisticsProvider.get();
            var updates = globalContextSync.getSyncData(file.aiCtx, statistics, cancellationToken);
            for (var update : updates)
            {
                if (Fields.LOCAL_FUNCTIONS.equals(update.field))
                {
                    update.hash = file.hash;
                }
            }

            if (futures.size() >= MaxConcurrentSyncs)
            {
                try
                {
                    futures.forEach(CompletableFuture::join);
                }
                catch (Exception error)
                {
                    log.logError(error);
                }
                finally
                {
                    futures.clear();
                }
            }

            var feature = globalContextSync.sync(file.aiCtx, updates, 5, statistics, cancellationToken)
                .whenComplete((result, error) -> {
                    if (result != null && result)
                    {
                        synchronized (lockObject)
                        {
                            filesToSync.remove(file);
                        }
                    }
                });

            futures.add(feature);
        }

        try
        {
            futures.forEach(CompletableFuture::join);
        }
        catch (Exception error)
        {
            log.logError(error);
        }

        if (!filesToSync.isEmpty())
        {
            return new Result(ProjectTrackingWorkflowState.SYNC, ShortDelay);
        }

        return new Result(ProjectTrackingWorkflowState.HASH, LongDelay);
    }

    private ProjectFile initProjectFile(ProjectFile projectFile)
    {
        projectFile.setState(state.files.get(projectFile.path));
        return projectFile;
    }

    private static class ProjectFile
    {
        private final String path;
        public AIContext aiCtx;
        public final IFile file;
        public LocalDateTime updateTime;
        public String hash;
        public long modificationStamp = -1;
        private boolean hasInitialState;

        public ProjectFile(AIContext aiCtx, String path, IFile file, LocalDateTime updateTime)
        {
            Preconditions.checkNotNull(aiCtx);
            Preconditions.checkNotNull(path);
            Preconditions.checkNotNull(file);
            Preconditions.checkNotNull(updateTime);
            this.aiCtx = aiCtx;
            this.path = path;
            this.file = file;
            this.updateTime = updateTime;
        }

        public GlobalContextFileState getState()
        {
            var state = new GlobalContextFileState();
            state.time = modificationStamp;
            state.hash = hash;
            return state;
        }

        public void setState(GlobalContextFileState state)
        {
            if (state == null)
            {
                return;
            }

            modificationStamp = state.time;
            hash = state.hash;
            hasInitialState = true;
        }

        public boolean hasState()
        {
            return hasInitialState;
        }

        public boolean isEmpty()
        {
            return path == null || hash == null || modificationStamp <= 0 || path.isBlank() || hash.isBlank();
        }

        public void updateState()
        {
            hasInitialState = false;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(path);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ProjectFile other = (ProjectFile)obj;
            return Objects.equals(path, other.path);
        }
    }

    private static class FileToTrack
    {
        public final AIContext aiCtx;

        public FileToTrack(AIContext aiCtx)
        {
            this.aiCtx = aiCtx;
        }
    }

    private static class ProjectFileComparator
        implements Comparator<ProjectFile>
    {
        @Override
        public int compare(ProjectFile file1, ProjectFile file2)
        {
            return file1.updateTime.compareTo(file2.updateTime);
        }
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
