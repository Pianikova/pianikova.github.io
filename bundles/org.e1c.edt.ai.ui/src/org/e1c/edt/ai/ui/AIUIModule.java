/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.ui;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

import org.e1c.edt.ai.AIModule;
import org.e1c.edt.ai.CodeCompletionActionHandler;
import org.e1c.edt.ai.CodeCompletionSession;
import org.e1c.edt.ai.ICodeCompletionActionHandler;
import org.e1c.edt.ai.ICodeCompletionSession;
import org.e1c.edt.ai.ICursorInfoProvider;
import org.e1c.edt.ai.ILog;
import org.e1c.edt.ai.ISettingsStore;
import org.e1c.edt.ai.IUISettings;
import org.e1c.edt.ai.IVersionProvider;
import org.e1c.edt.ai.context.IModuleProvider;
import org.e1c.edt.ai.context.ModuleProvider;
import org.e1c.edt.ai.ui.handlers.FixDialog;
import org.e1c.edt.ai.ui.handlers.IFixDialog;
import org.e1c.edt.ai.ui.preferences.PreferenceStoreToSettingsStoreAdapter;
import org.eclipse.jface.preference.IPreferenceStore;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.BindingAnnotation;
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
        bind(IVersionProvider.class).toInstance(activator);
        bind(IDispatcher.class).to(Dispatcher.class).in(Singleton.class);
        bind(IPreferenceStore.class).toInstance(activator.getPreferenceStore());
        bind(ISettingsStore.class).to(PreferenceStoreToSettingsStoreAdapter.class).in(Singleton.class);
        bind(UI.class).in(Singleton.class);
        bind(IUI.class).to(UI.class);
        bind(IdeApiHandler.class).in(Singleton.class);
        bind(Chat.class).in(Singleton.class);
        bind(IChat.class).to(Chat.class);
        bind(IChatDialog.class).to(Chat.class);
        bind(IAIContextProvider.class).to(AIContextProvider.class).in(Singleton.class);
        bind(IUISettings.class).to(UISettings.class).in(Singleton.class);
        bind(new TypeLiteral<ICodeCompletionViewModel<CodeCompletionContext>>() { /**/ }).to(CodeCompletionViewModel.class).in(Singleton.class);
        bind(IHintPainter.class).to(HintPainter.class);
        bind(IHotKeys.class).to(HotKeys.class).in(Singleton.class);
        bind(IUserActions.class).to(UserActions.class).in(Singleton.class);
        bind(new TypeLiteral<ICodeCompletionSession<CodeCompletionContext>>() { /**/ }).to(new TypeLiteral<CodeCompletionSession<CodeCompletionContext>>() { /**/ });
        bind(new TypeLiteral<ICodeCompletionActionHandler<CodeCompletionContext>>() { /**/ }).to(new TypeLiteral<CodeCompletionActionHandler<CodeCompletionContext>>() { /**/ });
        bind(ICursorInfoProvider.class).to(CursorInfoProvider.class).in(Singleton.class);
        bind(IFinalCodeFeedbackViewModel.class).to(FinalCodeFeedbackViewModel.class).in(Singleton.class);
        bind(IFeedbackPainter.class).to(FeedbackPainter.class);
        bind(IFeedbackDialog.class).to(FeedbackDialog.class);
        bind(IIssueFeedbackViewModel.class).to(IssueFeedbackViewModel.class);
        bind(IModuleProvider.class).annotatedWith(BaseModuleProvider.class).to(ModuleProvider.class);
        bind(IModuleProvider.class).to(CurrentEditorModuleProvider.class);
        bind(IFixDialog.class).to(FixDialog.class).in(Singleton.class);
        bind(IContentProvider.class).to(ContentProvider.class).in(Singleton.class);
        bind(IJavaScript.class).to(JavaScript.class).in(Singleton.class);
        // @formatter:on
    }


    @BindingAnnotation
    @Qualifier
    @Target({ FIELD, PARAMETER, METHOD })
    @Retention(RUNTIME)
    public @interface BaseModuleProvider
    {
        //
    }
}