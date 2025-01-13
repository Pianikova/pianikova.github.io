/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui.preferences;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Properties;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsStore;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.jface.preference.IPreferenceStore;

import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class PreferenceStoreToSettingsStoreAdapter implements ISettingsStore
{
    private static final String AI_PROPS_FILE_NAME = "ai.props"; //$NON-NLS-1$
    private static final int AI_UID_SIZE = 8;
    private final Object lock = new Object();
    private final ILog log;
    private final IPreferenceStore preferenceStore;
    private final IJson json;
    private Properties props;

    @Inject
    public PreferenceStoreToSettingsStoreAdapter(ILog log, IPreferenceStore preferenceStore, IJson json)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(preferenceStore);
        Preconditions.checkNotNull(json);
        this.log = log;
        this.preferenceStore = preferenceStore;
        this.json = json;
    }

    @Override
    public String getString(String key)
    {
        switch(key)
        {
            case ISettingsStore.CLIENT_UID:
                var curProps = getProps();
                var val = curProps.getProperty(key, "").trim(); //$NON-NLS-1$
                if (val.length() != AI_UID_SIZE)
                {
                    val = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, AI_UID_SIZE); //$NON-NLS-1$ //$NON-NLS-2$
                    curProps.setProperty(key, val);
                    saveProps();
                }

                return val;

            default:
                return preferenceStore.getString(key);
        }
    }

    @Override
    public int getInt(String key)
    {
        return preferenceStore.getInt(key);
    }

    @Override
    public boolean getBoolean(String key)
    {
        return preferenceStore.getBoolean(key);
    }

    private Properties getProps()
    {
        synchronized (lock)
        {
            if (props != null)
            {
                return props;
            }

            props = new Properties();
            try
            {
                var propsFilePath = getPropsFilePath();
                if (Files.exists(propsFilePath) && Files.isRegularFile(propsFilePath))
                {
                    var propsStr = Files.readString(propsFilePath);
                    var reader = new StringReader(propsStr);
                    props.load(reader);
                }
            }
            catch (IOException e)
            {
                log.logError(e);
            }

            return props;
        }
    }

    private void saveProps()
    {
        synchronized (lock)
        {
            var curProps = getProps();
            var writer = new StringWriter();
            try
            {
                curProps.store(writer, "AI properties"); //$NON-NLS-1$
                var propsFilePath = getPropsFilePath();
                Files.writeString(propsFilePath, writer.toString());
            }
            catch (IOException e)
            {
                log.logError(e);
            }
        }
    }

    private Path getPropsFilePath()
    {
        return Path.of(ConfigurationScope.INSTANCE.getLocation()
            .addTrailingSeparator()
            .append(AI_PROPS_FILE_NAME)
            .toFile()
            .getAbsolutePath());
    }

    @Override
    public <T> Optional<T> getValue(String key, Class<T> classOfT)
    {
        String value;
        switch (key)
        {
        case GLOBAL_CONTEXT:
            var curProps = getProps();
            value = curProps.getProperty(key, "").trim(); //$NON-NLS-1$
            break;

        default:
            value = preferenceStore.getString(key);
            break;
        }

        if (value == null)
        {
            return Optional.empty();
        }

        return json.deserialize(value, classOfT);
    }

    @Override
    public <T> void setValue(String key, T value)
    {
        var serializedValue = json.serialize(value);
        switch (key)
        {
        case GLOBAL_CONTEXT:
            var curProps = getProps();
            curProps.setProperty(key, serializedValue);
            saveProps();
            break;

        default:
            preferenceStore.setValue(key, serializedValue);
            break;
        }
    }
}
