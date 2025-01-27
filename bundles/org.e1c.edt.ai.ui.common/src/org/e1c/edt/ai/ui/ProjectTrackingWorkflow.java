/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.io.ByteArrayInputStream;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.e1c.edt.ai.AIContext;
import org.e1c.edt.ai.AIContextKind;
import org.e1c.edt.ai.ICancellationToken;
import org.e1c.edt.ai.IClock;
import org.e1c.edt.ai.IHashTools;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.IProjectIdProvider;
import org.e1c.edt.ai.IStatistics;
import org.e1c.edt.ai.assistent.model.ProjectId;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Provider;

class ProjectTrackingWorkflow
    implements IProjectTrackingWorkflow
{
    private final static int MaxConcurrentSyncs = 1;
    private final static Duration ExtraLongDelay = Duration.ofSeconds(15);
    private final static Duration LongDelay = Duration.ofSeconds(1);
    private final static Duration ShortDelay = Duration.ofMillis(10);
    private final static ProjectFileComparator ProjectFileComparator = new ProjectFileComparator();
    private final ILog log;
    private final Provider<IStatistics> statisticsProvider;
    private final IHashTools hashTools;
    private final IClock clock;
    private final IProjectIdProvider projectIdProvider;
    private final IGlobalContextSync globalContextSync;
    private final HashMap<String, ProjectFile> filesToHash = new HashMap<>();
    private final HashSet<ProjectFile> filesToSync = new HashSet<>();
    private final HashMap<String, FileToTrack> filesToTrack = new HashMap<>();
    private final CharBuffer buffer = CharBuffer.allocate(1024);
    private IProject project;
    private ProjectId projectId;
    private Set<String> hashes = new HashSet<>();
    private ProjectTrackingWorkflowState nextState = ProjectTrackingWorkflowState.INIT;

    @Inject
    public ProjectTrackingWorkflow(ILog log, Provider<IStatistics> statisticsProvider, IHashTools hashTools,
        IClock clock, IProjectIdProvider projectIdProvider,
        IGlobalContextSync globalContextSync)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(statisticsProvider);
        Preconditions.checkNotNull(hashTools);
        Preconditions.checkNotNull(clock);
        Preconditions.checkNotNull(projectIdProvider);
        Preconditions.checkNotNull(globalContextSync);
        this.log = log;
        this.statisticsProvider = statisticsProvider;
        this.hashTools = hashTools;
        this.clock = clock;
        this.projectIdProvider = projectIdProvider;
        this.globalContextSync = globalContextSync;
    }

    @Override
    public synchronized ProjectTrackingWorkflow initialize(IProject project, Set<String> hashes)
    {
        this.project = project;
        projectId = projectIdProvider.getProjectId(project);
        this.hashes = hashes;
        return this;
    }

    @Override
    public synchronized Duration nextState(IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
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
                result = hash(10, progressMonitor, cancellationToken);
                break;

            case SYNC:
                result = sync(20, progressMonitor, cancellationToken);
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
    public List<String> getHashes()
    {
        var result = new ArrayList<String>();
        for (var file : filesToHash.values())
        {
            if (file.sync)
            {
                result.add(file.hash);
            }
        }

        return result;
    }

    @Override
    public void track(AIContext aiCtx)
    {
        synchronized(filesToTrack)
        {
            log.trace("Track", () -> aiCtx.toString()); //$NON-NLS-1$
            filesToTrack.compute(aiCtx.getPath(), (key, prev) -> new FileToTrack(aiCtx));
        }
    }

    private Result init(IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
    {
        return new Result(ProjectTrackingWorkflowState.SCAN, ShortDelay);
    }

    private Result scan(IProgressMonitor progressMonitor, ICancellationToken cancellationToken)
        throws CoreException
    {
        progressMonitor.subTask(Messages.CodeCompletionBackgroundScanSubtaskName);
        final var hasFileToHash = new Boolean[1];
        hasFileToHash[0] = false;
        var filesToRemove = new ArrayList<ProjectFile>();
        for (var file : filesToHash.values())
        {
            if (!file.file.exists())
            {
                filesToRemove.add(file);
            }
        }

        for (var file : filesToRemove)
        {
            filesToHash.remove(file.path);
        }

        var now = clock.now();
        project.accept(resource -> {
            if (resource instanceof IFile)
            {
                var file = (IFile)resource;
                if ("bsl".equalsIgnoreCase(file.getFileExtension())) //$NON-NLS-1$)
                {
                    var path = file.getFullPath().makeRelative().toPortableString();
                    var aiCtx = new AIContext(projectId, AIContextKind.Common, 0, "", 0, path, "", 0); //$NON-NLS-1$//$NON-NLS-2$
                    if (!filesToHash.containsKey(path))
                    {
                        hasFileToHash[0] = true;
                        filesToHash.put(path, new ProjectFile(aiCtx, path, file, now));
                    }
                }

                return false;
            }

            if (resource instanceof IFolder)
            {
                var folder = (IFolder)resource;
                var pathSegments = folder.getProjectRelativePath().segments();
                if (pathSegments.length == 1)
                {
                    return "src".equalsIgnoreCase(pathSegments[0]); //$NON-NLS-1$
                }

                if (pathSegments.length == 2)
                {
                    return "CommonModules".equalsIgnoreCase(pathSegments[1]); //$NON-NLS-1$
                }

                return pathSegments.length > 2;
            }

            if (resource instanceof IProject)
            {
                return true;
            }

            return false;
        });

        return new Result(ProjectTrackingWorkflowState.HASH, hasFileToHash[0] ? ShortDelay : ExtraLongDelay);
    }

    private Result hash(int maxFiles, IProgressMonitor progressMonitor,
        ICancellationToken cancellationToken)
    {
        maxFiles = maxFiles - filesToSync.size();
        if (maxFiles <= 0)
        {
            return new Result(ProjectTrackingWorkflowState.SYNC, Duration.ofMillis(100));
        }

        var now = clock.now();
        synchronized (filesToTrack)
        {
            var processedFiles = new ArrayList<String>();
            for (var fileToTrack : filesToTrack.entrySet())
            {
                var path = fileToTrack.getKey();
                var file = filesToHash.get(path);
                if (file != null)
                {
                    if (!file.file.isAccessible())
                    {
                        filesToHash.remove(path);
                        processedFiles.add(path);
                        continue;
                    }

                    file.updateTime = now;
                    file.aiCtx = fileToTrack.getValue().aiCtx;
                    processedFiles.add(path);
                }
            }

            for (var processedFile : processedFiles)
            {
                filesToTrack.remove(processedFile);
            }
        }

        final var hasFileToSync = new Boolean[1];
        hasFileToSync[0] = false;
        var delay = Duration.ofSeconds(15);
        var filesToProcess =
            filesToHash.values()
                .stream()
                .filter(i -> Duration.between(i.updateTime, now).compareTo(delay) >= 0)
                .sorted(ProjectFileComparator)
                .limit(maxFiles)
                .collect(Collectors.toList());

        if (filesToProcess.isEmpty())
        {
            return new Result(ProjectTrackingWorkflowState.SCAN, LongDelay);
        }

        progressMonitor.beginTask(Messages.CodeCompletionBackgroundHashSubtaskName, filesToProcess.size());
        for (var file : filesToProcess)
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            try
            {
                file.sync = false;
                file.updateTime = now;
                MessageDigest hash;
                if (file.aiCtx.getKind() == AIContextKind.Common)
                {
                    if (!file.file.isAccessible())
                    {
                        continue;
                    }

                    var modificationStamp = file.file.getModificationStamp();
                    if (file.modificationStamp == modificationStamp)
                    {
                        file.sync = true;
                        continue;
                    }

                    file.modificationStamp = modificationStamp;
                    hash = hashTools.compute(file.file, buffer);
                }
                else
                {
                    try (var inputStream =
                        new ByteArrayInputStream(file.aiCtx.getSource().getBytes(StandardCharsets.UTF_8));)
                    {
                        hash = hashTools.compute(inputStream, StandardCharsets.UTF_8, buffer);
                    }
                }


                var hashStr = hashTools.format(hash, true);
                if (hashStr.equals(file.hash))
                {
                    file.sync = true;
                    continue;
                }

                file.hash = hashStr;
                if (hashes.contains(hashStr))
                {
                    file.sync = true;
                    continue;
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

    private Result sync(int maxFiles, IProgressMonitor progressMonitor,
        ICancellationToken cancellationToken)
    {
        var filesToProcess =
            filesToSync.stream().sorted(ProjectFileComparator).limit(maxFiles).collect(Collectors.toList());
        progressMonitor.beginTask(Messages.CodeCompletionBackgroundSyncSubtaskName, filesToProcess.size());
        var futures = new ArrayList<CompletableFuture<Boolean>>();
        for (var file : filesToProcess)
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            var statistics = statisticsProvider.get();
            var updates = globalContextSync.getSyncData(file.aiCtx, statistics, cancellationToken);

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

            var feature = globalContextSync.sync(file.aiCtx, updates, 2, statistics, cancellationToken)
                .whenComplete((result, error) -> {
                    if (result != null && result)
                    {
                        file.sync = true;
                        synchronized (filesToSync)
                        {
                            filesToSync.remove(file);
                        }
                    }

                    progressMonitor.worked(1);
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

    private static class ProjectFile
    {
        public AIContext aiCtx;
        private final String path;
        public final IFile file;
        public LocalDateTime updateTime;
        public String hash;
        public boolean sync = false;
        public long modificationStamp = -1;

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
