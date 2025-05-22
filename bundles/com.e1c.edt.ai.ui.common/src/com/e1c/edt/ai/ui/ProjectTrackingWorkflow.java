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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.e1c.edt.ai.AIContext;
import com.e1c.edt.ai.Fields;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IClock;
import com.e1c.edt.ai.IGlobalContext;
import com.e1c.edt.ai.IHashTools;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectIdProvider;
import com.e1c.edt.ai.IStatistics;
import com.e1c.edt.ai.IUISettings;
import com.e1c.edt.ai.assistent.IGlobalContextService;
import com.e1c.edt.ai.assistent.model.GlobalContextUpdate;
import com.e1c.edt.ai.assistent.model.ProjectId;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class ProjectTrackingWorkflow
    implements IProjectTrackingWorkflow
{
    private final static Duration LongDelay = Duration.ofSeconds(6);
    private final static Duration ShortDelay = Duration.ofMillis(10);
    private final ILog log;
    private final Provider<IStatistics> statisticsProvider;
    private final IHashTools hashTools;
    private final IClock clock;
    private final IProjectIdProvider projectIdProvider;
    private final IGlobalContextService globalContextService;
    private final IGlobalContextSync globalContextSync;
    private final IUISettings settings;
    private final IFileScaner fileScaner;
    private final IGlobalContext globalContext;
    private final HashSet<ProjectFile> filesToSync = new HashSet<>();
    private final HashMap<String, ProjectFile> filesToHash = new HashMap<>();
    private final ArrayList<ProjectFile> filesToTrack = new ArrayList<>();
    private final CharBuffer buffer = CharBuffer.allocate(1024);
    private IProject project;
    private ProjectId projectId;
    private ProjectTrackingWorkflowState nextState = ProjectTrackingWorkflowState.INIT;
    private int iterationCount = Integer.MAX_VALUE;
    private boolean initialStateSent;

    @Inject
    public ProjectTrackingWorkflow(ILog log, Provider<IStatistics> statisticsProvider, IHashTools hashTools,
        IClock clock, IProjectIdProvider projectIdProvider, IGlobalContextService globalContextService,
        IGlobalContextSync globalContextSync, IUISettings settings, IFileScaner fileScaner,
        IGlobalContext globalContext)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(statisticsProvider);
        Preconditions.checkNotNull(hashTools);
        Preconditions.checkNotNull(clock);
        Preconditions.checkNotNull(projectIdProvider);
        Preconditions.checkNotNull(globalContextService);
        Preconditions.checkNotNull(globalContextSync);
        Preconditions.checkNotNull(settings);
        Preconditions.checkNotNull(fileScaner);
        Preconditions.checkNotNull(globalContext);
        this.log = log;
        this.statisticsProvider = statisticsProvider;
        this.hashTools = hashTools;
        this.clock = clock;
        this.projectIdProvider = projectIdProvider;
        this.globalContextService = globalContextService;
        this.globalContextSync = globalContextSync;
        this.settings = settings;
        this.fileScaner = fileScaner;
        this.globalContext = globalContext;
    }

    @Override
    public ProjectTrackingWorkflow initialize(IProject project)
    {
        Preconditions.checkNotNull(project);
        this.project = project;
        projectId = projectIdProvider.getProjectId(project);
        iterationCount = Integer.MAX_VALUE;
        initialStateSent = true;
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
                result = sync(1000, progressMonitor, cancellationToken);
                break;
            }
        }
        catch (Exception error)
        {
            log.logError(error);
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
    public void track(AIContext aiCtx)
    {
        log.debug("Track", () -> aiCtx.toString()); //$NON-NLS-1$
        var path = aiCtx.getPath();
        var fileOnDisk = project.getFile(project.getProjectRelativePath().append(path).removeFirstSegments(1));
        if (fileOnDisk == null)
        {
            return;
        }

        synchronized (filesToTrack)
        {
            while (filesToTrack.size() > 1)
            {
                filesToTrack.remove(0);
            }

            if (filesToTrack.size() > 0 && filesToTrack.get(0).path.equals(path))
            {
                return;
            }

            filesToTrack.add(new ProjectFile(aiCtx, path, fileOnDisk, LocalDateTime.MIN));
            filesToHash.remove(path);
        }
    }

    private Result init(IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
    {
        if (iterationCount < 10)
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
        if (!settings.sendGlobalContext())
        {
            return new Result(ProjectTrackingWorkflowState.INIT, LongDelay);
        }

        List<IFile> files = fileScaner.scan(project);
        var now = clock.now();
        for (var file : files)
        {
            var path = file.getFullPath().makeRelative().toPortableString();
            filesToHash.computeIfAbsent(path,
                key -> new ProjectFile(new AIContext(projectId, 0, "", 0, key, "", 0, null), path, file, now));
        }

        var newFilesToHashCount = 0;
        synchronized (filesToTrack)
        {
            for (var file : filesToTrack)
            {
                if (file.getHash() == null)
                {
                    newFilesToHashCount++;
                }

                filesToHash.remove(file.path);
            }
        }

        for (var file : filesToHash.values())
        {
            if (file.getModificationStamp() <= 0 && file.getHash() == null)
            {
                newFilesToHashCount++;
            }
        }

        var newFilesToHashCountVal = newFilesToHashCount;
        log.debug("Scaned", () -> {
            var message = new StringBuilder();
            message.append("Files: ");
            message.append(files.size());
            message.append(System.lineSeparator());
            message.append("New files to hash: ");
            message.append(newFilesToHashCountVal);
            return files.size() + " files";
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
        maxFiles = maxFiles - filesToSync.size();
        if (maxFiles <= 0)
        {
            return new Result(ProjectTrackingWorkflowState.INIT, Duration.ofMillis(100));
        }

        var fileToSyncCount = 0;
        var delay = Duration.ofSeconds(3);
        var now = clock.now();
        var hashingFiles =
            filesToHash.values()
                .stream()
                .filter(file -> file.getAge(now).compareTo(delay) >= 0)
                .sorted(ProjectFile.COMPARATOR)
                .limit(maxFiles)
                .collect(Collectors.toList());

        synchronized (filesToTrack)
        {
            hashingFiles.addAll(filesToTrack);
        }

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
                    filesToHash.remove(file.path);
                }

                var prevModificationStamp = file.getModificationStamp();
                long newModificationStamp = -1;
                var prevHash = file.getHash();
                String newHash;
                var document = file.aiCtx.getDocument();
                if (document != null)
                {
                    try (var inputStream = new ByteArrayInputStream(document.get().getBytes(StandardCharsets.UTF_8));)
                    {
                        newHash =
                            hashTools.format(hashTools.compute(inputStream, StandardCharsets.UTF_8, buffer), true);
                        hashed++;
                    }
                }
                else
                {
                    if (!file.file.isAccessible())
                    {
                        continue;
                    }

                    newModificationStamp = file.file.getModificationStamp();
                    if (file.getModificationStamp() == newModificationStamp)
                    {
                        continue;
                    }

                    newHash = hashTools.format(hashTools.compute(file.file, buffer), true);
                    hashed++;
                }

                file.update(now, newHash, newModificationStamp);
                if (newHash.equals(prevHash))
                {
                    continue;
                }

                fileToSyncCount++;
                if (filesToSync.add(file))
                {
                    var modificationStamp = newModificationStamp;
                    log.debug("Sync required", () -> {
                        var message = new StringBuilder();
                        message.append("File: ");
                        message.append(file.path);

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
                        message.append(newHash);
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
            log.debug("Hashed", () -> hashedVal + " files");
        }

        if (fileToSyncCount > 0)
        {
            return new Result(ProjectTrackingWorkflowState.SYNC, ShortDelay);
        }

        if (!hashingFiles.isEmpty())
        {
            return new Result(ProjectTrackingWorkflowState.HASH, ShortDelay);
        }

        return new Result(ProjectTrackingWorkflowState.INIT, LongDelay);
    }

    @SuppressWarnings("nls")
    private Result sync(int maxFiles, IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
    {
        var filesToProcess =
            filesToSync.stream().sorted(ProjectFile.COMPARATOR).limit(maxFiles).collect(Collectors.toList());

        var filesUpdates = new ArrayList<GlobalContextUpdate>();
        for (var file : filesToProcess)
        {
            var update = new GlobalContextUpdate();
            update.field = Fields.LOCAL_FUNCTIONS;
            update.path = file.path;
            update.hash = file.getHash();
            filesUpdates.add(update);
        }

        Set<String> unknowFilePaths;
        try
        {
            unknowFilePaths =
                globalContextService.update(projectId, filesUpdates, 100, statisticsProvider.get(), cancellationToken)
                    .get()
                    .map(i -> {
                        var paths = new HashSet<String>();
                        for (var unknownValue : i.unknownValues)
                        {
                            paths.add(unknownValue.path);
                        }
                        for (var unknownKey : i.unknownKeys)
                        {
                            paths.add(unknownKey.path);
                        }
                        return paths;
                    })
                    .orElseGet(() -> new HashSet<>());
        }
        catch (Throwable error)
        {
            log.logError(error);
            return new Result(ProjectTrackingWorkflowState.HASH, LongDelay);
        }

        var files = new ArrayList<ProjectFile>();
        for (var file : filesToProcess)
        {
            if (unknowFilePaths.contains(file.path))
            {
                files.add(file);
                continue;
            }

            filesToSync.remove(file);
        }

        if (files.isEmpty())
        {
            return new Result(ProjectTrackingWorkflowState.HASH, ShortDelay);
        }

        log.debug("Syncing", () -> files.size() + " files");
        progressMonitor.beginTask(Messages.CodeCompletionBackgroundSyncSubtaskName, files.size());
        var statistics = statisticsProvider.get();
        var feature = CompletableFuture.completedFuture(true);
        var updates = new ArrayList<GlobalContextUpdate>();
        for (var file : files)
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            var fileUpdates = globalContext.getUpdates(projectId, file.path, !initialStateSent, statistics, cancellationToken);
            initialStateSent = true;
            for (var update : fileUpdates)
            {
                if (Fields.LOCAL_FUNCTIONS.equals(update.field))
                {
                    update.hash = file.getHash();
                }
            }

            synchronized (updates)
            {
                updates.addAll(fileUpdates);
            }

            feature = feature
                .thenCompose(i -> {
                    ArrayList<GlobalContextUpdate> latestUpdates;
                    synchronized (updates)
                    {
                        latestUpdates = new ArrayList<>(updates);
                        updates.clear();
                    }

                    if (latestUpdates.isEmpty())
                    {
                        return CompletableFuture.completedFuture(true);
                    }

                    return globalContextSync.syncUpdates(projectId, latestUpdates, 5, statistics, cancellationToken)
                        .whenComplete((result, error) -> {
                            progressMonitor.worked(1);
                            if (result != null && result)
                            {
                                filesToSync.remove(file);
                            }
                        });
                });
        }

        feature.join();
        if (!filesToSync.isEmpty())
        {
            return new Result(ProjectTrackingWorkflowState.SYNC, ShortDelay);
        }

        if (!filesToProcess.isEmpty())
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
