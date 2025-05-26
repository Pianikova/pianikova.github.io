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
import java.util.HashSet;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocumentExtension4;

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
    private final static Duration LongDelay = Duration.ofSeconds(6);
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
    private final ConcurrentHashMap<String, ProjectFile> filesToHash = new ConcurrentHashMap<>();
    private final CharBuffer buffer = CharBuffer.allocate(1024);
    private IProject project;
    private ProjectId projectId;
    private ProjectTrackingWorkflowState nextState = ProjectTrackingWorkflowState.INIT;
    private int iterationCount = Integer.MAX_VALUE;
    private boolean initialStateSent;

    @Inject
    public ProjectTrackingWorkflow(ILog log, Provider<IStatistics> statisticsProvider, IHashTools hashTools,
        IClock clock, IProjectIdProvider projectIdProvider, IGlobalContextSync globalContextSync, IUISettings settings,
        IFileScaner fileScaner)
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

        filesToHash.put(path, new ProjectFile(aiCtx, path, fileOnDisk, LocalDateTime.MIN));
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
                key -> new ProjectFile(new AIContext(projectId, key), key, file, now));
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
                if (!file.aiCtx.isDisposed() && document != null && document instanceof IDocumentExtension4)
                {
                    var docExtension = (IDocumentExtension4)document;
                    newModificationStamp = docExtension.getModificationStamp();
                    if (file.getModificationStamp() == newModificationStamp)
                    {
                        continue;
                    }

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
            return new Result(ProjectTrackingWorkflowState.HASH, LongDelay);
        }

        return new Result(ProjectTrackingWorkflowState.INIT, LongDelay);
    }

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
            filesToSync.remove(file);
        }

        globalContextSync
            .syncUpdates(projectId, initialStateSent, filesUpdates, 5, statisticsProvider.get(), cancellationToken)
            .join();
        initialStateSent = true;
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
