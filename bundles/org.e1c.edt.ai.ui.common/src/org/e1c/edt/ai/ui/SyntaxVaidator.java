/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.e1c.edt.ai.ILog;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.LazyStringInputStream;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.common.io.Files;
import com.google.inject.Inject;
import com.google.inject.Provider;

class SyntaxVaidator
    implements ISyntaxVaidator
{
    private final ILog log;
    private static final Map<String, String> PARSE_OPTIONS;
    private final Provider<XtextResourceSet> resourceSetProvider;

    static
    {
        PARSE_OPTIONS = Maps.newHashMap();
        PARSE_OPTIONS.put(org.eclipse.xtext.resource.XtextResource.OPTION_ENCODING, StandardCharsets.UTF_8.name());
    }

    @Inject
    public SyntaxVaidator(ILog log, Provider<XtextResourceSet> resourceSetProvider)
    {
        Preconditions.checkNotNull(log);
        Preconditions.checkNotNull(resourceSetProvider);
        this.log = log;
        this.resourceSetProvider = resourceSetProvider;
    }

    @Override
    public int getValidCodeSize(String filePath, CharSequence code)
    {
        var fileExtension = Files.getFileExtension(filePath);
        if (fileExtension != null && !fileExtension.isBlank())
        {
            var codeStream = getAsStream(code);
            var resourceSet = resourceSetProvider.get();
            var uriToUse = computeUnusedUri(resourceSet, fileExtension);
            try
            {
                var optionalResult = parse(codeStream, uriToUse, PARSE_OPTIONS, resourceSet);
                if (optionalResult.isPresent())
                {
                    var result = optionalResult.get();
                    var errors = result.getSyntaxErrors();
                    var errorOffset = code.length();
                    for (var error : errors)
                    {
                        var offset = error.getOffset();
                        if (offset < errorOffset)
                        {
                            errorOffset = offset;
                        }
                    }

                    var validCodeSize = errorOffset + 1;
                    if (validCodeSize < code.length())
                    {
                        return validCodeSize;
                    }

                    return code.length();
                }
            }
            catch (Exception error)
            {
                log.logError(error);
            }
        }

        return code.length();
    }

    private static URI computeUnusedUri(ResourceSet resourceSet, String fileExtension)
    {
        for (var i = 0; i < Integer.MAX_VALUE; i++)
        {
            var syntheticUri = URI.createURI("__synthetic" + i + "." + fileExtension); //$NON-NLS-1$ //$NON-NLS-2$
            if (resourceSet.getResource(syntheticUri, false) == null)
            {
                return syntheticUri;
            }
        }

        throw new IllegalStateException();
    }

    private Resource createResource(InputStream in, URI uriToUse, Map<?, ?> options, ResourceSet resourceSet)
        throws IOException
    {
        var resourceServiceProvider =
            IResourceServiceProvider.Registry.INSTANCE.getResourceServiceProvider(uriToUse);
        var resourceFactory = resourceServiceProvider.get(IResourceFactory.class);
        var resource = resourceFactory.createResource(uriToUse);
        resourceSet.getResources().add(resource);
        resource.load(in, options);
        return resource;
    }

    private Optional<IParseResult> parse(InputStream in, URI uriToUse, Map<?, ?> options, ResourceSet resourceSet)
        throws IOException
    {
        var resource = createResource(in, uriToUse, options, resourceSet);
        if (resource instanceof XtextResource)
        {
            return Optional.ofNullable(((XtextResource)resource).getParseResult());
        }

        return Optional.empty();
    }

    private InputStream getAsStream(CharSequence text)
    {
        return new LazyStringInputStream(text == null ? "" : text.toString()); //$NON-NLS-1$
    }
}