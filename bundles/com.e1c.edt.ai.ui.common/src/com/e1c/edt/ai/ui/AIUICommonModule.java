/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui;

import com.e1c.edt.ai.AIModule;
import com.e1c.edt.ai.CodeCompletionActionHandler;
import com.e1c.edt.ai.CodeCompletionSession;
import com.e1c.edt.ai.ICodeCompletionActionHandler;
import com.e1c.edt.ai.ICodeCompletionSession;
import com.e1c.edt.ai.IContentSourceProvider;
import com.e1c.edt.ai.ICursorInfoProvider;
import com.e1c.edt.ai.IGlobalContextManager;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsSetter;
import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.tools.MCPToolsModule;
import com.e1c.edt.ai.ui.handlers.CodeTools;
import com.e1c.edt.ai.ui.handlers.FixDialog;
import com.e1c.edt.ai.ui.handlers.ICodeTools;
import com.e1c.edt.ai.ui.handlers.IFixDialog;
import com.e1c.edt.ai.ui.preferences.PreferenceStoreToSettingsStoreAdapter;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;

public class AIUICommonModule
    extends AbstractModule
{
    public static final String PARAMETERS = "Parameters"; //$NON-NLS-1$
    public static final String URL = "URL"; //$NON-NLS-1$

    @Override
    protected void configure()
    {
        // @formatter:off
        install(new AIModule());
        install(new MCPToolsModule());

        // inirializables
        var initializableBinder = Multibinder.newSetBinder(binder(), IInitializable.class);
        initializableBinder.addBinding().to(UI.class);
        initializableBinder.addBinding().to(ContextMenuInterceptor.class);
        initializableBinder.addBinding().to(ClipboardManager.class);
        initializableBinder.addBinding().to(DialogsEnhancer.class);
        initializableBinder.addBinding().to(ResourceListener.class);
        initializableBinder.addBinding().to(UpdateService.class);
        initializableBinder.addBinding().to(Notificator.class);
        initializableBinder.addBinding().to(ActiveProjectTracker.class);

        bind(UI.class).in(Singleton.class);
        bind(IUI.class).to(UI.class);
        bind(ContextMenuInterceptor.class).in(Singleton.class);
        bind(ClipboardManager.class).in(Singleton.class);
        bind(IClipboard.class).to(ClipboardManager.class);
        bind(DialogsEnhancer.class).in(Singleton.class);
        bind(ResourceListener.class).in(Singleton.class);
        bind(UpdateService.class).in(Singleton.class);
        bind(Notificator.class).in(Singleton.class);
        bind(ActiveProjectTracker.class).in(Singleton.class);

        // view enhancers
        var viewEnhancerBinder = Multibinder.newSetBinder(binder(), IViewEnhancer.class);
        viewEnhancerBinder.addBinding().to(StagingViewEnhancer.class);

        bind(IDispatcher.class).to(Dispatcher.class).in(Singleton.class);
        bind(ISettingsStore.class).to(PreferenceStoreToSettingsStoreAdapter.class).in(Singleton.class);
        bind(IdeApiHandler.class);
        bind(Chat.class).in(Singleton.class);
        bind(IChat.class).to(Chat.class);
        bind(IChatDialog.class).to(Chat.class);
        bind(IAIContextProvider.class).to(AIContextProvider.class).in(Singleton.class);
        bind(Settings.class).in(Singleton.class);
        bind(ISettings.class).to(Settings.class);
        bind(ISettingsSetter.class).to(Settings.class);
        bind(new TypeLiteral<ICodeCompletionViewModel<CodeCompletionContext>>() { /**/ }).to(CodeCompletionViewModel.class).in(Singleton.class);
        bind(IHintPainter.class).to(HintPainter.class).in(Singleton.class);
        bind(IVerticalRulerPainter.class).to(VerticalRulerPainter.class).in(Singleton.class);
        bind(IHotKeys.class).to(HotKeys.class).in(Singleton.class);
        bind(IUserActions.class).to(UserActions.class).in(Singleton.class);
        bind(new TypeLiteral<ICodeCompletionSession<CodeCompletionContext>>() { /**/ }).to(new TypeLiteral<CodeCompletionSession<CodeCompletionContext>>() { /**/ });
        bind(new TypeLiteral<ICodeCompletionActionHandler<CodeCompletionContext>>() { /**/ }).to(new TypeLiteral<CodeCompletionActionHandler<CodeCompletionContext>>() { /**/ });
        bind(ICursorInfoProvider.class).to(CursorInfoProvider.class).in(Singleton.class);
        bind(IFeedbackDialog.class).to(FeedbackDialog.class);
        bind(IIssueFeedbackViewModel.class).to(IssueFeedbackViewModel.class);
        bind(IFixDialog.class).to(FixDialog.class).in(Singleton.class);
        bind(IContentProvider.class).to(ContentProvider.class).in(Singleton.class);
        bind(IJavaScript.class).to(JavaScript.class).in(Singleton.class);
        bind(ICursorInfoProvider.class).to(CursorInfoProvider.class).in(Singleton.class);
        bind(IGlobalContextManager.class).to(GlobalContextManager.class).in(Singleton.class);
        bind(ISyntaxVaidator.class).to(SyntaxVaidator.class).in(Singleton.class);
        bind(IGlobalContextTracker.class).to(GlobalContextTracker.class).in(Singleton.class);
        bind(IProjectTrackingWorkflow.class).to(ProjectTrackingWorkflow.class);
        bind(IGlobalContextSync.class).to(GlobalContextSync.class).in(Singleton.class);
        bind(IProposalsProvider.class).to(ProposalsProvider.class).in(Singleton.class);
        bind(TextWidgetInfo.class).in(Singleton.class);
        bind(ITextWidgetInfoUpdater.class).to(TextWidgetInfo.class);
        bind(ITextWidgetInfoProvider.class).to(TextWidgetInfo.class);
        bind(ICodeTools.class).to(CodeTools.class).in(Singleton.class);
        bind(IVerticalRulerManager.class).to(VerticalRulerManager.class).in(Singleton.class);
        bind(IGCTools.class).to(GCTools.class).in(Singleton.class);
        bind(IUINotificationService.class).to(UINotificationService.class);
        bind(IFileScaner.class).to(FileScaner.class).in(Singleton.class);
        bind(IPluginUpdateService.class).to(PluginUpdateService.class);
        bind(IReflection.class).to(Reflection.class).in(Singleton.class);
        bind(IReflection.class).to(Reflection.class).in(Singleton.class);
        bind(IWidgets.class).to(Widgets.class).in(Singleton.class);
        bind(IGitTools.class).to(GitTools.class).in(Singleton.class);
        bind(IGitActions.class).to(GitActions.class).in(Singleton.class);
        bind(IResourceProvider.class).to(ResourceProvider.class).in(Singleton.class);
        bind(IFileSystem.class).to(FileSystem.class).in(Singleton.class);
        bind(IProjectTrackingDeltaVisitor.class).to(ProjectTrackingDeltaVisitor.class).in(Singleton.class);
        bind(ITextActions.class).to(TextActions.class).in(Singleton.class);
        bind(IStateService.class).to(StateService.class).in(Singleton.class);
        bind(IContentSourceProvider.class).to(ContentSourceProvider.class).in(Singleton.class);
        bind(IEdtLinkHandler.class).to(EdtLinkHandler.class).in(Singleton.class);
        bind(IEditorPositionManager.class).to(EditorPositionManager.class).in(Singleton.class);
        bind(IThemeManager.class).to(ThemeManager.class).in(Singleton.class);
        bind(INotifications.class).to(Notifications.class).in(Singleton.class);
        bind(IWeb.class).to(Web.class).in(Singleton.class);
        bind(IPreferences.class).to(Preferences.class).in(Singleton.class);
        // @formatter:on
    }
}