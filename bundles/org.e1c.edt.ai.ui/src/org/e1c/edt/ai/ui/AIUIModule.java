/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.AIContextSettings;
import org.e1c.edt.ai.AIModule;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.ui.preferences.PreferenceStoreToSettingsStoreAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.source.SourceViewer;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;

public class AIUIModule
    extends AbstractModule
{
    public static final String PARAMETERS = "Parameters"; //$NON-NLS-1$
    public static final String URL = "URL"; //$NON-NLS-1$

    private Activator activator;

    public AIUIModule(Activator activator)
    {
        Preconditions.checkNotNull(activator);
        this.activator = activator;
    }

    @Override
    protected void configure()
    {
        // @formatter:off
        install(new AIModule());
        bind(ILog.class).toInstance(activator);
        bind(IDispatcher.class).to(Dispatcher.class).in(Singleton.class);
        bind(IPreferenceStore.class).toInstance(activator.getPreferenceStore());
        bind(ISettingsStore.class).to(PreferenceStoreToSettingsStoreAdapter.class).in(Singleton.class);
        bind(UI.class).in(Singleton.class);
        bind(IUI.class).to(UI.class);
        bind(IdeApiHandler.class).in(Singleton.class);
        bind(Chat.class).in(Singleton.class);
        bind(IChat.class).to(Chat.class);
        bind(IChatDialog.class).to(Chat.class);
        bind(AIContextSettings.class).toProvider(AIContextSettingsProvider.class);
        bind(new TypeLiteral<IAIContextProvider<Integer>>() { /**/ }).to(AIContextProvider.class).in(Singleton.class);
        bind(new TypeLiteral<IAIContextProvider<SourceViewer>>() { /**/ }).to(AISourceViewerContextProvider.class).in(Singleton.class);
        bind(new TypeLiteral<IAIContextProvider<AISourceContext>>() { /**/ }).to(AISourceContextProvider.class).in(Singleton.class);
        bind(new TypeLiteral<ISyntaxWalker<StringSerializerContext>>() { /**/ }).to(new TypeLiteral<BasicPathSyntaxWalker<StringSerializerContext>>() { /**/ }).in(Singleton.class);
        bind(new TypeLiteral<ISyntaxVisitor<StringSerializerContext>>() { /**/ }).to(StringSerializerVisitor.class).in(Singleton.class);
        bind(IUISettings.class).to(UISettings.class).in(Singleton.class);
        bind(ICodeCompletionViewModel.class).to(CodeCompletionViewModel.class).in(Singleton.class);
        bind(IHintPainter.class).to(HintPainter.class);
        bind(IHotKeys.class).to(HotKeys.class).in(Singleton.class);
        // @formatter:on
    }
}
