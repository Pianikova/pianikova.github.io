/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.eclipse.core.runtime.Plugin;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslMultiLineCommentDocumentationProvider;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.filesystem.IQualifiedNameFilePathConverter;
import com._1c.g5.v8.dt.core.model.IModelEditingSupport;
import com._1c.g5.v8.dt.core.model.IModelObjectFactory;
import com._1c.g5.v8.dt.core.naming.ITopObjectFqnGenerator;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IDerivedDataManagerProvider;
import com._1c.g5.v8.dt.core.platform.IEditingLanguageManager;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.core.platform.management.IDtHostResourceManager;
import com._1c.g5.v8.dt.form.generator.IFormFieldGenerator;
import com._1c.g5.v8.dt.form.generator.IFormGenerator;
import com._1c.g5.v8.dt.form.service.datasourceinfo.IDataSourceInfoAssociationService;
import com._1c.g5.v8.dt.md.IExternalPropertyManagerRegistry;
import com._1c.g5.v8.dt.md.refactoring.core.IMdRefactoringService;
import com._1c.g5.v8.dt.platform.version.IRuntimeVersionSupport;
import com._1c.g5.v8.dt.search.core.text.ITextSearchIndexProvider;
import com._1c.g5.v8.dt.validation.marker.v2.IMarkerManagerV2;
import com._1c.g5.wiring.AbstractServiceAwareModule;
import com.e1c.edt.ai.ICodePartsProvider;
import com.e1c.edt.ai.ICodeProvider;
import com.e1c.edt.ai.IConfigurationParametersProvider;
import com.e1c.edt.ai.IContextEntities;
import com.e1c.edt.ai.IEditingSupport;
import com.e1c.edt.ai.IFiles;
import com.e1c.edt.ai.IMarkdownUtils;
import com.e1c.edt.ai.IMarkersProvider;
import com.e1c.edt.ai.IMcpTool;
import com.e1c.edt.ai.IProjectBuilder;
import com.e1c.edt.ai.IProjectDetailsProvider;
import com.e1c.edt.ai.IProjectParametersProvider;
import com.e1c.edt.ai.MarkdownUtils;
import com.e1c.edt.ai.context.tools.FindMcpTool;
import com.e1c.edt.ai.context.tools.GetObjectMcpTool;
import com.e1c.edt.ai.context.tools.metadata.EditMetadataMcpTool;
import com.e1c.edt.ai.context.tools.IMethodListProvider;
import com.e1c.edt.ai.context.tools.MarkersProvider;
import com.e1c.edt.ai.context.tools.MetadataBindingProvider;
import com.e1c.edt.ai.context.tools.MetadataManualCatalog;
import com.e1c.edt.ai.context.tools.MethodListProvider;
import com.e1c.edt.ai.context.tools.ProjectBuilder;
import com.e1c.edt.ai.tools.IJShellBindingProvider;
import com.e1c.edt.ai.tools.IJShellManualProvider;
import com.google.inject.Singleton;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;

class ContextModule
    extends AbstractServiceAwareModule
{
    public ContextModule(Plugin plugin)
    {
        super(plugin);
    }

    @Override
    protected void doConfigure()
    {
        // @formatter:off
        bind(IEntitiesWalker.class).to(EntitiesWalker.class).in(Singleton.class);
        bind(IRelatedEntities.class).to(RelatedEntities.class).in(Singleton.class);
        bind(IEntityInfo.class).to(EntityInfo.class).in(Singleton.class);
        bind(IContextEntities.class).to(EntityInfo.class).in(Singleton.class);
        bind(IV8Model.class).to(V8Model.class).in(Singleton.class);
        bind(IIdFactory.class).to(IdFactory.class).in(Singleton.class);
        bind(BslMultiLineCommentDocumentationProvider.class).toInstance(new BslMultiLineCommentDocumentationProvider());
        bind(ICommentFactory.class).to(CommentFactory.class).in(Singleton.class);
        bind(IEntityFactory.class).to(EntityFactory.class).in(Singleton.class);
        bind(IFormWalker.class).to(FormWalker.class).in(Singleton.class);
        bind(ICodePartsProvider.class).to(CodePartsProvider.class).in(Singleton.class);
        bind(IDispatcher.class).to(Dispatcher.class).in(Singleton.class);
        bind(ICodeProvider.class).to(CodeProvider.class).in(Singleton.class);
        bind(IBmPovider.class).to(BmPovider.class).in(Singleton.class);
        bind(IBmObjectProvider.class).to(BmObjectProvider.class).in(Singleton.class);
        bind(IFiles.class).to(Files.class).in(Singleton.class);
        bind(IConfigurationParametersProvider.class).to(ConfigurationParametersProvider.class).in(Singleton.class);
        bind(IProjectParametersProvider.class).to(ConfigurationParametersProvider.class).in(Singleton.class);
        bind(IEditingSupport.class).to(EditingSupport.class).in(Singleton.class);
        bind(IMarkdownUtils.class).to(MarkdownUtils.class).in(Singleton.class);
        bind(IMethodListProvider.class).to(MethodListProvider.class).in(Singleton.class);
        // EDT project "build": waits for the background DD validation to settle and flushes markers.
        bind(IProjectBuilder.class).to(ProjectBuilder.class).in(Singleton.class);
        var projectDetailsProviderBinder = Multibinder.newSetBinder(binder(), IProjectDetailsProvider.class);
        projectDetailsProviderBinder.addBinding().to(ConfigurationParametersProvider.class);
        bind(MessageDigest.class).toProvider(() -> {
            try
            {
                return MessageDigest.getInstance("MD5");//$NON-NLS-1$
            }
            catch (NoSuchAlgorithmException e)
            {
                return null;
            }
        });
        bind(IModuleProvider.class).annotatedWith(Names.named("BaseModuleProvider")).to(ModuleProvider.class).in(Singleton.class); //$NON-NLS-1$

        // MCP tools
        var toolBinder = Multibinder.newSetBinder(binder(), IMcpTool.class);
        toolBinder.addBinding().to(FindMcpTool.class);
        toolBinder.addBinding().to(GetObjectMcpTool.class);
        toolBinder.addBinding().to(EditMetadataMcpTool.class);

        // Markers providers
        var markersProviderBinder = Multibinder.newSetBinder(binder(), IMarkersProvider.class);
        markersProviderBinder.addBinding().to(MarkersProvider.class);

        // JShell binding providers
        var jshellBindingProviderBinder = Multibinder.newSetBinder(binder(), IJShellBindingProvider.class);
        jshellBindingProviderBinder.addBinding().to(MetadataBindingProvider.class);
        var jshellManualProviderBinder = Multibinder.newSetBinder(binder(), IJShellManualProvider.class);
        jshellManualProviderBinder.addBinding().to(MetadataManualCatalog.class);

        // Services
        bind(IExternalPropertyManagerRegistry.class).toService();
        bind(IBmModelManager.class).toService();
        bind(IResourceLookup.class).toService();
        bind(IDataSourceInfoAssociationService.class).toService();
        bind(IV8ProjectManager.class).toService();
        bind(IRuntimeVersionSupport.class).toService();
        bind(IProjectFileSystemSupportProvider.class).toService();
        bind(IQualifiedNameFilePathConverter.class).toService();
        bind(IDtHostResourceManager.class).toService();
        bind(ITextSearchIndexProvider.class).toService();
        bind(IModelEditingSupport.class).toService();
        bind(IMarkerManagerV2.class).toService();
        bind(IDerivedDataManagerProvider.class).toService();
        bind(ITopObjectFqnGenerator.class).toService();
        bind(IModelObjectFactory.class).toService();
        bind(IFormGenerator.class).toService();
        bind(IFormFieldGenerator.class).toService();
        bind(IEditingLanguageManager.class).toService();
        bind(IMdRefactoringService.class).toService();
        // @formatter:on
    }
}
