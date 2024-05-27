/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import org.e1c.edt.ai.AIModule;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.ui.preferences.PreferenceStoreToSettingsStoreAdapter;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.ISelectionListener;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

public class AIUIModule
    extends AbstractModule
{
    public static final String PARAMETERS = "Parameters"; //$NON-NLS-1$
    public static final String URL = "URL"; //$NON-NLS-1$

    private Activator activator;

    public AIUIModule(Activator activator)
    {
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
        bind(IUI.class).to(UI.class).in(Singleton.class);
        bind(IdeApiHandler.class).in(Singleton.class);
        bind(Chat.class).in(Singleton.class);
        bind(IChat.class).to(Chat.class);
        bind(IChatDialog.class).to(Chat.class);
        bind(IAIContextProvider.class).to(AIContextProvider.class).in(Singleton.class);
        bind(ICodeCompletion.class).to(CodeCompletion.class).in(Singleton.class);
        bind(AIPartListener.class).in(Singleton.class);
        bind(IPartListener2.class).to(AIPartListener.class);
        bind(ISelectionListener.class).to(AIPartListener.class);
        bind(IUISettings.class).to(UISettings.class).in(Singleton.class);
        bind(IHintPainter.class).toProvider(HintPainterProvider.class);
        bind(ICodeCompletionViewModel.class).to(CodeCompletionViewModel.class);
        // @formatter:on
    }
}
