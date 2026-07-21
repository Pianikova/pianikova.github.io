/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.FrameworkUtil;

import com.e1c.edt.ai.IJson;
import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

/**
 * File-based {@link IGlobalContextStateStore}. Stores one JSON file per project under the bundle state location
 * (<code>.metadata/.plugins/com.e1c.edt.ai.ui.common/globalcontext/&lt;project&gt;.json</code>), so the state lives
 * outside the project tree (never committed to VCS) and is easy to remove per project.
 */
public class GlobalContextStateStore
    implements IGlobalContextStateStore
{
    private static final String STATE_DIRECTORY = "globalcontext"; //$NON-NLS-1$
    private static final String FILE_EXTENSION = ".json"; //$NON-NLS-1$

    private final ILog log;
    private final IJson json;

    @Inject
    public GlobalContextStateStore(ILog log, IJson json)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(json);
        this.log = log;
        this.json = json;
    }

    @Override
    public Map<String, GlobalContextFileState> load(IProject project)
    {
        Preconditions.checkNotNull(project);
        try
        {
            var file = stateFile(project);
            if (file == null || !Files.exists(file))
            {
                return new HashMap<>();
            }

            var content = Files.readString(file, StandardCharsets.UTF_8);
            var state = json.deserialize(content, GlobalContextState.class).orElse(null);
            if (state == null || state.files == null)
            {
                return new HashMap<>();
            }

            return new HashMap<>(state.files);
        }
        catch (Exception error)
        {
            log.logError(error);
            return new HashMap<>();
        }
    }

    @Override
    public void save(IProject project, Map<String, GlobalContextFileState> state)
    {
        Preconditions.checkNotNull(project);
        Preconditions.checkNotNull(state);
        try
        {
            var file = stateFile(project);
            if (file == null)
            {
                return;
            }

            Files.createDirectories(file.getParent());

            var holder = new GlobalContextState();
            holder.files = state;
            var content = json.serialize(holder);

            // Write to a temporary sibling and move into place so a crash mid-write can't leave a truncated file.
            var temp = file.resolveSibling(file.getFileName() + ".tmp"); //$NON-NLS-1$
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            try
            {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            }
            catch (IOException atomicUnsupported)
            {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        }
        catch (Exception error)
        {
            log.logError(error);
        }
    }

    @Override
    public void delete(IProject project)
    {
        Preconditions.checkNotNull(project);
        try
        {
            var file = stateFile(project);
            if (file != null)
            {
                Files.deleteIfExists(file);
            }
        }
        catch (Exception error)
        {
            log.logError(error);
        }
    }

    private Path stateFile(IProject project)
    {
        var bundle = FrameworkUtil.getBundle(getClass());
        if (bundle == null)
        {
            return null;
        }

        var location = Platform.getStateLocation(bundle);
        return Paths.get(location.toOSString(), STATE_DIRECTORY, fileName(project));
    }

    private static String fileName(IProject project)
    {
        var name = project.getName();
        // Replace only characters that are illegal in Windows/POSIX file names; keep Unicode letters (e.g.
        // Cyrillic) so the file stays human-readable. The reserved set is < > : " / \ | ? * and control chars.
        var sanitized = name.replaceAll("[\\\\/:*?\"<>|\\x00-\\x1F]", "_"); //$NON-NLS-1$ //$NON-NLS-2$
        // Append a hash of the original name so distinct names that sanitize to the same string don't collide.
        return sanitized + "-" + Integer.toHexString(name.hashCode()) + FILE_EXTENSION; //$NON-NLS-1$
    }
}
