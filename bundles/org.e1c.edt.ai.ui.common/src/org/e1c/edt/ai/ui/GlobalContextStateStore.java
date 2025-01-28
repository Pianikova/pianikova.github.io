/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashSet;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.ILog;
import org.eclipse.core.runtime.preferences.ConfigurationScope;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

class GlobalContextStateStore implements IGlobalContextStateStore
{
    private static final String AI_PROPS_FILE_NAME = "ai.json"; //$NON-NLS-1$
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
    public GlobalContextState load()
    {
        String stateContents;
        try
        {
            var file = getFilePath();
            if (Files.exists(getFilePath()))
            {
                stateContents = Files.readString(file, StandardCharsets.UTF_8);
                var optionalState = json.deserialize(stateContents, GlobalContextState.class);
                if (optionalState.isPresent())
                {
                    var state = optionalState.get();
                    if (state.hashes != null)
                    {
                        return state;
                    }
                }
            }
        }
        catch (IOException error)
        {
            log.logError(error);
        }

        var newState = new GlobalContextState();
        newState.hashes = new HashSet<>();
        return newState;
    }

    @Override
    public void save(GlobalContextState state)
    {
        var stateContents = json.serialize(state);
        try
        {
            Files.writeString(getFilePath(), stateContents, StandardCharsets.UTF_8, StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING);
        }
        catch (IOException error)
        {
            log.logError(error);
        }
    }

    private Path getFilePath()
    {
        return Path.of(ConfigurationScope.INSTANCE.getLocation()
            .addTrailingSeparator()
            .append(AI_PROPS_FILE_NAME)
            .toFile()
            .getAbsolutePath());
    }
}
