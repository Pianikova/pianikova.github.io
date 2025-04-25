/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.xtext.parser.IParseResult;
import org.eclipse.xtext.resource.IResourceFactory;
import org.eclipse.xtext.resource.IResourceServiceProvider;
import org.eclipse.xtext.resource.XtextResource;
import org.eclipse.xtext.resource.XtextResourceSet;
import org.eclipse.xtext.util.LazyStringInputStream;

import com.e1c.edt.ai.CodeMethod;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
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
    public String getValidHint(CodeMethod method, String sourceCode, int offset, String hintText,
        ICancellationToken cancellationToken)
    {
        var source = sourceCode;
        var end = method.getEndOffest();
        var len = source.length();
        if (len == 0)
        {
            return hintText;
        }

        if (end >= len)
        {
            end = len - 1;
        }

        var code = source.substring(method.getStartOffest(), end);
        var validCodeSize = getValidHintSize(code, hintText, offset - method.getStartOffest(),
            cancellationToken);

        var validHintLines = hintText.substring(0, validCodeSize);
        if (hintText.length() != validHintLines.length())
        {
            log.warning("Syntax check " + cancellationToken, () -> { //$NON-NLS-1$
                var message = new StringBuilder();
                message.append("Original hint: ["); //$NON-NLS-1$
                message.append(hintText);
                message.append(']');
                message.append(System.lineSeparator());
                message.append(System.lineSeparator());

                message.append("Valid hint:    ["); //$NON-NLS-1$
                message.append(validHintLines);
                message.append(']');
                message.append(System.lineSeparator());
                message.append(System.lineSeparator());

                message.append("Method: "); //$NON-NLS-1$
                message.append(method.getUniqueName());

                return message.toString();
            });
        }

        return validHintLines;
    }

    private int getValidHintSize(String code, String hint, int offset,
        ICancellationToken cancellationToken)
    {
        var paseResult = parse(code, hint, offset);
        if (paseResult.isEmpty())
        {
            return hint.length();
        }

        var errorOffset = getMinErrorOffset(paseResult.get(), cancellationToken) - offset;
        if (errorOffset < 0 || errorOffset > hint.length())
        {
            return hint.length();
        }

        return errorOffset;
    }

    private int getMinErrorOffset(IParseResult parseResult, ICancellationToken cancellationToken)
    {
        var errors = parseResult.getSyntaxErrors();
        var minErrorOffset = -1;
        for (var error : errors)
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            var offset = error.getOffset();
            if (minErrorOffset == -1 || offset < minErrorOffset)
            {
                minErrorOffset = offset;
            }
        }

        return minErrorOffset;
    }

    private Optional<IParseResult> parse(String code, String hint, int offset)
    {
        var fullCode = new StringBuilder(code);
        if (fullCode.length() < offset)
        {
            return Optional.empty();
        }

        fullCode.insert(offset, hint);
        var fileExtension = ".bsl"; //$NON-NLS-1$
        if (fileExtension != null && !fileExtension.isBlank())
        {
            var codeStream = getAsStream(fullCode.toString());
            var resourceSet = resourceSetProvider.get();
            var uriToUse = computeUnusedUri(resourceSet, fileExtension);
            try
            {
                return parse(codeStream, uriToUse, PARSE_OPTIONS, resourceSet);
            }
            catch (Exception error)
            {
                log.logError(error);
            }
        }

        return Optional.empty();
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

    private Optional<Resource> createResource(InputStream in, URI uriToUse, Map<?, ?> options, ResourceSet resourceSet)
        throws IOException
    {
        var resourceServiceProvider =
            IResourceServiceProvider.Registry.INSTANCE.getResourceServiceProvider(uriToUse);
        if (resourceServiceProvider == null)
        {
            return Optional.empty();
        }

        var resourceFactory = resourceServiceProvider.get(IResourceFactory.class);
        if (resourceFactory == null)
        {
            return Optional.empty();
        }

        var resource = resourceFactory.createResource(uriToUse);
        if (resource == null)
        {
            return Optional.empty();
        }

        resourceSet.getResources().add(resource);
        resource.load(in, options);
        return Optional.of(resource);
    }

    private Optional<IParseResult> parse(InputStream in, URI uriToUse, Map<?, ?> options, ResourceSet resourceSet)
        throws IOException
    {
        return createResource(in, uriToUse, options, resourceSet).map(resource -> {
            if (resource instanceof XtextResource)
            {
                return ((XtextResource)resource).getParseResult();
            }

            return null;
        });
    }

    private InputStream getAsStream(CharSequence text)
    {
        return new LazyStringInputStream(text == null ? "" : text.toString()); //$NON-NLS-1$
    }
}