/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import org.e1c.edt.ai.IJson;
import org.e1c.edt.ai.Json;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslMultiLineCommentDocumentationProvider;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class ContextModule
    extends AbstractModule
{
    @Override
    protected void configure()
    {
        // @formatter:off
        bind(IJson.class).to(Json.class).in(Singleton.class);
        bind(IEntitiesWalker.class).to(EntitiesWalker.class).in(Singleton.class);
        bind(IRelatedEntities.class).to(RelatedEntities.class).in(Singleton.class);
        bind(IEntityInfo.class).to(EntityInfo.class).in(Singleton.class);
        bind(IV8Model.class).to(V8Model.class).in(Singleton.class);
        bind(IIdFactory.class).to(IdFactory.class).in(Singleton.class);
        bind(BslMultiLineCommentDocumentationProvider.class).toInstance(new BslMultiLineCommentDocumentationProvider());
        bind(ICommentFactory.class).to(CommentFactory.class).in(Singleton.class);
        // @formatter:on
    }
}
