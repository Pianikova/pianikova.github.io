/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.Invocation;
import com._1c.g5.v8.dt.bsl.model.Method;
import com.google.common.base.Preconditions;
import com.google.common.base.Splitter;
import com.google.inject.Inject;

class IdFactory
    implements IIdFactory
{
    private static String MAX_INT = Integer.toString(Integer.MAX_VALUE);
    private final IV8Model v8Model;

    @Inject
    public IdFactory(IV8Model v8Model)
    {
        Preconditions.checkNotNull(v8Model);
        this.v8Model = v8Model;
    }

    @Override
    public String createNodeId(String path, ICompositeNode node)
    {
        try {
            var requestPathUrl = new URL("file", "", -1, path); //$NON-NLS-1$ //$NON-NLS-2$
            var start = node.getTotalOffset();
            var finish = node.getTotalEndOffset();
            return requestPathUrl.toString() + "?start=" + start + "&finish=" + finish; //$NON-NLS-1$ //$NON-NLS-2$
        }
        catch (MalformedURLException e)
        {
            return ""; //$NON-NLS-1$
        }
    }

    @Override
    public String createObjectId(String path, EObject eObject, ICancellationToken cancellationToken)
    {
        if (eObject instanceof FeatureAccess)
        {
            var featureAccess = (FeatureAccess)eObject;
            var types = v8Model.getTypes(featureAccess);
            if (!types.isEmpty())
            {
                var urls = new StringBuilder();
                for (var type : types)
                {
                    var resource = type.eResource();
                    if (resource != null)
                    {
                        var uri = resource.getURI();
                        if (uri != null)
                        {
                            if (urls.length() != 0)
                            {
                                urls.append(';');
                            }

                            urls.append(uri);
                        }
                    }
                }
            }
        }

        var node = v8Model.getNode(eObject);
        if (eObject instanceof Invocation)
        {
            var invocation = (Invocation)eObject;
            var methodAccessFeatureOptional = v8Model.getMethodFeature(invocation.getMethodAccess(), cancellationToken);
            if (methodAccessFeatureOptional.isPresent())
            {
                var methodAccessFeature = methodAccessFeatureOptional.get();
                if (methodAccessFeature instanceof Method)
                {
                    var method = (Method)methodAccessFeature;
                    return method.getUniqueName();
                }

                if (methodAccessFeature instanceof com._1c.g5.v8.dt.mcore.Method)
                {
                    var method = (com._1c.g5.v8.dt.mcore.Method)methodAccessFeature;
                    var resource = method.eResource();
                    if (resource != null)
                    {
                        var uri = resource.getURI();
                        if (uri != null)
                        {
                            var id = new StringBuilder();
                            id.append(uri);
                            id.append('.');
                            id.append(method.getName());
                            id.append('(');
                            var hasParam = false;
                            for(var paramSet: method.getParamSet())
                            {
                                for(var param: paramSet.getParams())
                                {
                                    if(hasParam)
                                    {
                                        id.append(',');
                                    }
                                    else
                                    {
                                        hasParam = true;
                                    }

                                    id.append(param.getName());
                                }
                            }

                            id.append(')');
                            id.append(method.environments());
                            return id.toString();
                        }
                    }

                }
            }
        }

        return createNodeId(path, node);
    }

    @Override
    @SuppressWarnings("nls")
    public Optional<SourceSpan> getNodeId(String nodeId)
    {
        URL url;
        try
        {
            url = new URL(nodeId);
        }
        catch (MalformedURLException e)
        {
            return Optional.empty();
        }

        var path = url.getPath();
        var params = Splitter.on('&').trimResults().withKeyValueSeparator('=').split(url.getQuery());
        var start = Integer.parseInt(params.getOrDefault("start", "0"));
        var finish = Integer.parseInt(params.getOrDefault("finish", MAX_INT));
        return Optional.of(new SourceSpan(path, start, finish));
    }
}
