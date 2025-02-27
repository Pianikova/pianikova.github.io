/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.context;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.e1c.edt.ai.ICodePartsProvider;
import org.e1c.edt.ai.ICodeProvider;
import org.e1c.edt.ai.IContextEntities;
import org.eclipse.core.runtime.Plugin;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslMultiLineCommentDocumentationProvider;
import com._1c.g5.v8.dt.core.filesystem.IProjectFileSystemSupportProvider;
import com._1c.g5.v8.dt.core.platform.IBmModelManager;
import com._1c.g5.v8.dt.core.platform.IResourceLookup;
import com._1c.g5.v8.dt.core.platform.IV8ProjectManager;
import com._1c.g5.v8.dt.form.service.datasourceinfo.IDataSourceInfoAssociationService;
import com._1c.g5.v8.dt.md.IExternalPropertyManagerRegistry;
import com._1c.g5.wiring.AbstractServiceAwareModule;
import com.google.inject.Singleton;
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
        bind(IExternalPropertyManagerRegistry.class).toService();
        bind(IBmModelManager.class).toService();
        bind(IResourceLookup.class).toService();
        bind(IDataSourceInfoAssociationService.class).toService();
        bind(IV8ProjectManager.class).toService();
        bind(IProjectFileSystemSupportProvider.class).toService();
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
        // @formatter:on
    }
}
