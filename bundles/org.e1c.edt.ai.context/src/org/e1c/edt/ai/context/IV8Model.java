/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.e1c.edt.ai.ICancellationToken;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.xtext.nodemodel.ICompositeNode;
import org.eclipse.xtext.util.Pair;

import com._1c.g5.v8.bm.core.IBmObject;
import com._1c.g5.v8.bm.integration.IBmModel;
import com._1c.g5.v8.dt.bsl.documentation.comment.BslDocumentationComment;
import com._1c.g5.v8.dt.bsl.model.BslContextDefMethod;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.FeatureEntry;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.typesytem.VariableTypeStateProviderCollector;
import com._1c.g5.v8.dt.bsl.resource.TypesComputer;
import com._1c.g5.v8.dt.mcore.Property;
import com._1c.g5.v8.dt.mcore.Type;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.mcore.util.Environments;

public interface IV8Model
{
    IBmObject getBmObjectOwner(IBmModel bmModel, EObject object);

    List<Type> getTypes(VariableTypeStateProviderCollector typeStateProviders, ICompositeNode node);

    List<TypeItem> getTypes(EObject eObject);

    Collection<Pair<Collection<Property>, TypeItem>> getProperties(Collection<TypeItem> types, Resource resource);

    List<FeatureEntry> getFeatureEntries(FeatureAccess featureAccess);

    Optional<String> getPath(FeatureAccess featureAccess);

    TypesComputer getTypesComputer();

    Environments getEnvironments(EObject eObject);

    List<String> getComment(EObject eObject);

    BslDocumentationComment getComment(Method method, boolean oldFormat);

    BslDocumentationComment getComment(BslContextDefMethod method, boolean oldFormat);

    ICompositeNode getNode(EObject eObject);

    List<Type> getTypes(FeatureAccess featureAccess);

    Optional<EObject> getMethodFeature(FeatureAccess methodAccess, ICancellationToken cancellationToken);

    <T> T getResourceService(Class<T> type);
}
