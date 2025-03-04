/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.preferences;

import java.awt.Desktop;
import java.net.URI;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.preference.StringFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.e1c.edt.ai.IDefaultSettings;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.IValidator;
import com.e1c.edt.ai.ui.AIUICommonModule;
import com.e1c.edt.ai.ui.BaseActivator;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/**
 * This class contains fields on the preferences page with AI chat settings.
 * Parameters set on this page could be used when creating a new chat.
 * @see ClientAI
 * @author Bogdan Sushkov
 *
 */
public class ClientAIPreferencePage
    extends FieldEditorPreferencePage
    implements IWorkbenchPreferencePage
{
    private final Image SPLASH = createImage("icons/obj16/splash.png"); //$NON-NLS-1$

    @Inject
    ILog log;
    @Inject
    @Named(AIUICommonModule.URL)
    IValidator<String> urlValidator;
    @Inject
    @Named(AIUICommonModule.PARAMETERS)
    IValidator<String> parametersValidator;
    @Inject
    IPreferenceStore preferenceStore;
    @Inject
    IDefaultSettings defaultSettings;

    public ClientAIPreferencePage()
    {
        super(GRID);
        BaseActivator.injectMembers(this);
        setPreferenceStore(preferenceStore);
    }

    /**
     * Creates the field editors. Field editors are abstractions of
     * the common GUI blocks needed to manipulate various types
     * of preferences. Each field editor knows how to save and
     * restore itself.
     */
    @Override
    public void createFieldEditors()
    {
        var parent = getFieldEditorParent();

        addField(
            new StringFieldEditor(ISettingsStore.CLIENT_TOKEN, Messages.ClientAIPreferencePage_Client_Token, parent));

        addField(
            new BooleanFieldEditor(ISettingsStore.CODE_COMPLETION,
                Messages.ClientAIPreferencePage_CodeCompletitionEnabled,
                parent));

        addField(new BooleanFieldEditor(ISettingsStore.CONTINUOUS_CODE_COMPLETION,
            Messages.ClientAIPreferencePage_ContinuousCodeCompletition, parent));

        var codeCompletionLinesCount = new IntegerFieldEditor(ISettingsStore.CODE_COMPLETION_LINES_COUNT,
            Messages.ClientAIPreferencePage_CodeCompletionLinesCount, parent);
        codeCompletionLinesCount.setValidRange(1, ISettingsStore.MAX_CODE_COMPLETION_LINES_COUNT);
        addField(codeCompletionLinesCount);

        @SuppressWarnings("nls")
        String[][] languages = {
            { Messages.ClientAIPreferencePage_Language_Default, "" },
            { Messages.ClientAIPreferencePage_Language_English, "english" },
            { Messages.ClientAIPreferencePage_Language_Russian, "russian" } };

        addField(
            new ComboFieldEditor(ISettingsStore.LANGUAGE, Messages.ClientAIPreferencePage_Language, languages, parent));

        addField(new ValidatingStringFieldEditor(ISettingsStore.PARAMETERS,
            Messages.ClientAIPreferencePage_Parameters, parent, parametersValidator));
    }

    @Override
    public void init(IWorkbench workbench)
    {
        // Empty stub
    }

    @SuppressWarnings("nls")
    @Override
    protected Control createContents(Composite parent)
    {
        var control = super.createContents(parent);

        var pluginLink = new Link(parent, SWT.NONE);
        pluginLink
            .setText("<a href=\"" + defaultSettings.getHomePage() + "\">" + defaultSettings.getHomePage() + "</a>");
        pluginLink.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                try
                {
                    var url = defaultSettings.getHomePage(); //
                    if (Desktop.isDesktopSupported())
                    {
                        var desktop = Desktop.getDesktop();
                        if (desktop.isSupported(Desktop.Action.BROWSE))
                        {
                            desktop.browse(new URI(url));
                        }
                    }
                }
                catch (Exception error)
                {
                    log.logError(error);
                }
            }
        });

        var iconLabel = new Label(parent, SWT.NONE);
        iconLabel.setImage(SPLASH);

        return control;
    }

    private static Image createImage(String path)
    {
        var descriptor = ImageDescriptor
            .createFromURL(FileLocator.find(BaseActivator.getDefault().getBundle(), new Path(path), null));
        return descriptor.createImage();
    }
}
