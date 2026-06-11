/**
 *
 */
package com.e1c.edt.ai.ui;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

import org.osgi.framework.FrameworkUtil;

import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

public class ResourceProvider implements IResourceProvider
{
    private final ILog log;
    private final ISettings settings;

    @Inject
    public ResourceProvider(ILog log, ISettings settings)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(settings);
        this.log = log;
        this.settings = settings;
    }

    @Override
    public Optional<String> getTextResource(String filePath)
    {
        var optionalResources = settings.getResources();
        if (optionalResources.isPresent())
        {
            var path = Paths.get(optionalResources.get(), filePath);
            if (Files.exists(path))
            {
                try
                {
                    return Optional.ofNullable(Files.readString(path, StandardCharsets.UTF_8));
                }
                catch (IOException error)
                {
                    log.logError(error);
                }
            }
        }

        var bundle = FrameworkUtil.getBundle(getClass());
        var url = bundle.getEntry(filePath);
        if (url == null)
        {
            log.logError("Resource not found: " + filePath); //$NON-NLS-1$
            return Optional.empty();
        }

        try (var inputStream = url.openStream();
            var reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8)))
        {

            StringBuilder content = new StringBuilder();
            char[] buffer = new char[8192]; // 8KB buffer (adjust size as needed)
            int charsRead;

            while ((charsRead = reader.read(buffer)) != -1)
            {
                content.append(buffer, 0, charsRead);
            }

            return Optional.of(content.toString());
        }
        catch (IOException error)
        {
            log.logError(error);
            return Optional.empty();
        }
    }

    @Override
    public Set<String> listChildNames(String dirPath)
    {
        var names = new LinkedHashSet<String>();

        var optionalResources = settings.getResources();
        if (optionalResources.isPresent())
        {
            var dir = Paths.get(optionalResources.get(), dirPath);
            if (Files.isDirectory(dir))
            {
                try (Stream<java.nio.file.Path> stream = Files.list(dir))
                {
                    stream.forEach(child -> names.add(child.getFileName().toString()));
                }
                catch (IOException error)
                {
                    log.logError(error);
                }
            }
        }

        var bundle = FrameworkUtil.getBundle(getClass());
        var normalized = dirPath.endsWith("/") ? dirPath : dirPath + "/"; //$NON-NLS-1$ //$NON-NLS-2$
        var entries = bundle.getEntryPaths(normalized);
        if (entries != null)
        {
            while (entries.hasMoreElements())
            {
                var name = lastSegment(entries.nextElement());
                if (!name.isEmpty())
                {
                    names.add(name);
                }
            }
        }

        return names;
    }

    private static String lastSegment(String entryPath)
    {
        var trimmed = entryPath.endsWith("/") ? entryPath.substring(0, entryPath.length() - 1) : entryPath; //$NON-NLS-1$
        var slash = trimmed.lastIndexOf('/');
        return slash < 0 ? trimmed : trimmed.substring(slash + 1);
    }
}
