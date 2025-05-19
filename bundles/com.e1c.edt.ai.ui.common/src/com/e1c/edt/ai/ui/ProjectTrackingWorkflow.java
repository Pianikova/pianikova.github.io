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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.e1c.edt.ai.AIContext;
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
    private final static Duration ExtraLongDelay = Duration.ofSeconds(5);
    private final static Duration LongDelay = Duration.ofSeconds(1);
    private final static Duration ShortDelay = Duration.ofMillis(10);
    private final ILog log;
    private final Provider<IStatistics> statisticsProvider;
    private final IHashTools hashTools;
    private final IClock clock;
    private final IProjectIdProvider projectIdProvider;
    private final IGlobalContextSync globalContextSync;
    private final IUISettings settings;
    private final IFileScaner fileScaner;
    private final HashSet<ProjectFile> filesToSync = new HashSet<>();
    private final HashMap<String, ProjectFile> filesToHash = new HashMap<>();
    private final ArrayList<ProjectFile> filesToTrack = new ArrayList<>();
    private final CharBuffer buffer = CharBuffer.allocate(1024);
    private IProject project;
    private ProjectId projectId;
    private ProjectTrackingWorkflowState nextState = ProjectTrackingWorkflowState.INIT;
    private boolean initial = true;

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
    public ProjectTrackingWorkflow initialize(IProject project)
    {
        Preconditions.checkNotNull(project);
        this.project = project;
        projectId = projectIdProvider.getProjectId(project);
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
        log.debug("Track", () -> aiCtx.toString()); //$NON-NLS-1$
        var path = aiCtx.getPath();
        var fileOnDisk = project.getFile(project.getProjectRelativePath().append(path).removeFirstSegments(1));
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
        return new Result(ProjectTrackingWorkflowState.SCAN, ShortDelay);
    }

    @SuppressWarnings("nls")
    private Result scan(IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
        throws CoreException
    {
        progressMonitor.subTask(Messages.CodeCompletionBackgroundScanSubtaskName);
        if (settings.sendGlobalContext())
        {
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

            return new Result(ProjectTrackingWorkflowState.HASH, newFilesToHashCount > 0 ? ShortDelay : ExtraLongDelay);
        }

        return new Result(ProjectTrackingWorkflowState.SCAN, ExtraLongDelay);
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

        return new Result(ProjectTrackingWorkflowState.SCAN, LongDelay);
    }

    @SuppressWarnings("nls")
    private Result sync(int maxFiles, IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
    {
        var filesToProcess =
            filesToSync.stream().sorted(ProjectFile.COMPARATOR).limit(maxFiles).collect(Collectors.toList());

        if (filesToProcess.size() > 0)
        {
            log.debug("Syncing", () -> filesToProcess.size() + " files");
        }

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
        var newUpdates = new ArrayList<GlobalContextUpdate>();
        var statistics = statisticsProvider.get();
        for (var file : filesToProcess)
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            progressMonitor.worked(1);

            if (!unknowFilePaths.contains(file.path))
            {
                synchronized (filesToSync)
                {
                    filesToSync.remove(file);
                }

                continue;
            }

            var updates = globalContextSync.getSyncData(projectId, file.path, statistics, initial, cancellationToken);
            initial = false;
            for (var update : updates)
            {
                if (Fields.LOCAL_FUNCTIONS.equals(update.field))
                {
                    update.hash = file.getHash();
                }

                newUpdates.add(update);
            }

        }

        if (!newUpdates.isEmpty())
        {
            try
            {
                var success = globalContextSync.sync(projectId, newUpdates, 5, statistics, cancellationToken).get();
                if (success)
                {
                    synchronized (filesToSync)
                    {
                        filesToSync.clear();
                    }

                    return new Result(ProjectTrackingWorkflowState.SYNC, ShortDelay);
                }
            }
            catch (InterruptedException | ExecutionException error)
            {
                log.logError(error);
            }
        }

        return new Result(ProjectTrackingWorkflowState.HASH, LongDelay);
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
