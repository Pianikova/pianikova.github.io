/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.semantic;

import java.util.List;
import java.util.Optional;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.nodemodel.ICompositeNode;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslDocumentationComment;
import com._1c.g5.v8.dt.bsl.model.BslContextDefMethod;
import com._1c.g5.v8.dt.bsl.model.FeatureAccess;
import com._1c.g5.v8.dt.bsl.model.FeatureEntry;
import com._1c.g5.v8.dt.bsl.model.Method;
import com._1c.g5.v8.dt.bsl.model.Module;
import com._1c.g5.v8.dt.bsl.model.typesytem.VariableTypeStateProviderCollector;
import com._1c.g5.v8.dt.bsl.resource.TypesComputer;
import com._1c.g5.v8.dt.mcore.Type;
import com._1c.g5.v8.dt.mcore.TypeItem;
import com._1c.g5.v8.dt.mcore.util.Environments;

public interface IV8Model
{
    Optional<Module> getModule(String filePath);

    Optional<Type> getLastType(VariableTypeStateProviderCollector typeStateProviders);

    List<TypeItem> getTypes(EObject eObject);

    List<FeatureEntry> getFeatureEntries(FeatureAccess featureAccess);

    Optional<String> getPath(FeatureAccess featureAccess);

    TypesComputer getTypesComputer();

    Environments getEnvironments(EObject eObject);

    List<String> getComment(EObject eObject);

    BslDocumentationComment getComment(Method method, boolean oldFormat);

    BslDocumentationComment getComment(BslContextDefMethod method, boolean oldFormat);

    ICompositeNode getNode(EObject eObject);

    Optional<Type> getType(FeatureAccess featureAccess);

    Optional<EObject> getMethodFeature(FeatureAccess methodAccess);
}
