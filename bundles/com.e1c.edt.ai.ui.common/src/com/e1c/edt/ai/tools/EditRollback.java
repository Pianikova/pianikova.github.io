/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.ResourcesPlugin;

import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.IEditRollback;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.IProjectTools;
import com.e1c.edt.ai.ui.IDispatcher;
import com.e1c.edt.ai.ui.IFileSystem;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

/**
 * Stateless implementation of {@link IEditRollback}. Reads the current file content, applies the
 * inverse of the original Edit via {@link IContentReplacer}, and writes the result back through
 * the same channels the Edit tool itself uses (Eclipse document layer for project files; raw
 * filesystem otherwise). Refuses without changes whenever the post-edit fragment cannot be found
 * unambiguously, so intermediate edits made after the original call are never silently destroyed.
 */
@Singleton
public class EditRollback
    implements IEditRollback
{
    private final IContentReplacer contentReplacer;
    private final IProjectTools projectTools;
    private final IContentSourceProvider contentSourceProvider;
    private final IDispatcher dispatcher;
    private final IFileSystem fileSystem;
    private final ILog log;

    @Inject
    public EditRollback(IContentReplacer contentReplacer, IProjectTools projectTools,
        IContentSourceProvider contentSourceProvider, IDispatcher dispatcher, IFileSystem fileSystem, ILog log)
    {
        Preconditions.checkNotNull(contentReplacer);
        Preconditions.checkNotNull(projectTools);
        Preconditions.checkNotNull(contentSourceProvider);
        Preconditions.checkNotNull(dispatcher);
        Preconditions.checkNotNull(fileSystem);
        Preconditions.checkNotNull(log);
        this.contentReplacer = contentReplacer;
        this.projectTools = projectTools;
        this.contentSourceProvider = contentSourceProvider;
        this.dispatcher = dispatcher;
        this.fileSystem = fileSystem;
        this.log = log;
    }

    @SuppressWarnings("nls")
    @Override
    public boolean rollback(String path, String oldContent, String newContent, boolean replaceAll)
    {
        if (path == null || path.isBlank())
        {
            log.logError("rollback: blank path");
            return false;
        }
        if (oldContent == null || newContent == null)
        {
            log.logError("rollback: null old/new content for " + path);
            return false;
        }
        // Pure-deletion edits (newContent empty) cannot be inverted from text alone — the empty
        // string has no anchor to reinsert at. Refuse explicitly rather than producing garbage.
        if (newContent.isEmpty())
        {
            log.logError("rollback: cannot invert deletion (empty newContent) for " + path);
            return false;
        }

        String detectedProjectName = projectTools.determineProjectName(path);
        boolean isProjectFile = detectedProjectName != null && !detectedProjectName.isBlank();

        if (isProjectFile)
        {
            return rollbackProjectFile(path, detectedProjectName, oldContent, newContent, replaceAll);
        }
        return rollbackPlainFile(path, oldContent, newContent, replaceAll);
    }

    @SuppressWarnings("nls")
    private boolean rollbackProjectFile(String path, String projectName, String oldContent, String newContent,
        boolean replaceAll)
    {
        var root = ResourcesPlugin.getWorkspace().getRoot();
        var project = root.getProject(projectName);
        if (project == null || !project.exists() || !project.isOpen())
        {
            log.logError("rollback: project unavailable: " + projectName);
            return false;
        }

        var projectFile = projectTools.getProjectFile(project, path);
        if (projectFile.isEmpty())
        {
            log.logError("rollback: file not in project: " + path);
            return false;
        }

        var fileDocOptional = contentSourceProvider.getFileDocument(projectFile.get());
        if (fileDocOptional.isEmpty())
        {
            log.logError("rollback: cannot open file document: " + path);
            return false;
        }
        var fileDocument = fileDocOptional.get();
        var document = fileDocument.getDocument();

        var optionalCurrent = dispatcher.dispatch(() -> document.get());
        if (optionalCurrent.isEmpty())
        {
            log.logError("rollback: cannot read content: " + path);
            return false;
        }
        var current = optionalCurrent.get();

        // Inverse replacement: find newContent (what the Edit produced) and put oldContent back.
        var result = contentReplacer.replace(current, newContent, oldContent, System.lineSeparator(), replaceAll);
        if (!result.isSuccess())
        {
            log.logError("rollback: post-edit fragment not unique/missing — file diverged: " + path);
            return false;
        }

        var optionalError = dispatcher.dispatch(() -> {
            try
            {
                fileDocument.setContent(result.getUpdatedContent());
                fileDocument.save();
                return null;
            }
            catch (Exception e)
            {
                return e;
            }
        });

        if (optionalError.isPresent() && optionalError.get() != null)
        {
            log.logError(optionalError.get());
            return false;
        }
        return true;
    }

    @SuppressWarnings("nls")
    private boolean rollbackPlainFile(String path, String oldContent, String newContent, boolean replaceAll)
    {
        try
        {
            if (!fileSystem.fileExists(path))
            {
                log.logError("rollback: file does not exist: " + path);
                return false;
            }

            byte[] data = fileSystem.readAllBytes(path);
            String current = new String(data, StandardCharsets.UTF_8);
            var result = contentReplacer.replace(current, newContent, oldContent, System.lineSeparator(), replaceAll);
            if (!result.isSuccess())
            {
                log.logError("rollback: post-edit fragment not unique/missing — file diverged: " + path);
                return false;
            }
            fileSystem.writeAllBytes(path, result.getUpdatedContent().getBytes(StandardCharsets.UTF_8));
            return true;
        }
        catch (IOException e)
        {
            log.logError(e);
            return false;
        }
    }
}
