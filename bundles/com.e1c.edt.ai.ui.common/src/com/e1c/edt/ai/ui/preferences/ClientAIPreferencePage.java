/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.preferences;

import java.awt.Desktop;
import java.net.URI;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
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
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.e1c.edt.ai.IClientTokenValidator;
import com.e1c.edt.ai.IDefaultSettings;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.ISettings;
import com.e1c.edt.ai.ISettingsSetter;
import com.e1c.edt.ai.ISettingsStore;
import com.e1c.edt.ai.IValidator;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.assistent.IStateService;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
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

    @SuppressWarnings("nls")
    private static final String[][] LANGUAGES = {
        { Messages.ClientAIPreferencePage_Language_Default, "" },
        { Messages.ClientAIPreferencePage_Language_English, "english" },
        { Messages.ClientAIPreferencePage_Language_Russian, "russian" } };

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
    @Inject
    IStateService stateService;
    @Inject
    ISettings settings;
    @Inject
    ISettingsSetter settingsSetter;
    @Inject
    IClientTokenValidator clientTokenValidator;

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

        var tokenField =
            new StringFieldEditor(ISettingsStore.CLIENT_TOKEN, Messages.ClientAIPreferencePage_Client_Token, parent);
        var tokenLabel = tokenField.getLabelControl(parent);
        tokenLabel.setToolTipText(Messages.ClientAIPreferencePage_Client_Token_Tooltip);
        var TokenText = tokenField.getTextControl(getFieldEditorParent());
        TokenText.setEchoChar('*');
        addField(tokenField);

        var policyCombo = new PolicyComboFieldEditor(parent);
        var policyComboLabel = policyCombo.getLabelControl(parent);
        policyComboLabel.setToolTipText(Messages.ClientAIPreferencePage_CodeCompletionPolicy_Tooltip);
        addField(policyCombo);

        var codeCompletionLinesCount = new IntegerFieldEditor(ISettingsStore.CODE_COMPLETION_LINES_COUNT,
            Messages.ClientAIPreferencePage_CodeCompletionLinesCount, parent);
        codeCompletionLinesCount.setValidRange(1, ISettingsStore.MAX_CODE_COMPLETION_LINES_COUNT);
        var codeCompletionLinesCountLabel = codeCompletionLinesCount.getLabelControl(parent);
        codeCompletionLinesCountLabel
            .setToolTipText(Messages.ClientAIPreferencePage_CodeCompletionLinesCount_Tooltip);
        addField(codeCompletionLinesCount);

        var comboField =
            new ComboFieldEditor(ISettingsStore.LANGUAGE, Messages.ClientAIPreferencePage_Language, LANGUAGES, parent);
        var comboLabel = comboField.getLabelControl(parent);
        comboLabel.setToolTipText(Messages.ClientAIPreferencePage_Language_Tooltip);
        addField(comboField);

        var validatorField = new ValidatingStringFieldEditor(ISettingsStore.PARAMETERS,
            Messages.ClientAIPreferencePage_Parameters, parent, parametersValidator);
        var validatorLabel = validatorField.getLabelControl(parent);
        validatorLabel.setToolTipText(Messages.ClientAIPreferencePage_Parameters_Tooltip);
        addField(validatorField);
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

    @Override
    public void dispose()
    {
        SPLASH.dispose();
        super.dispose();
    }

    @Override
    public boolean performOk()
    {
        var result = super.performOk();
        var token = settings.getClientToken();
        if (clientTokenValidator.isValid(token) && settings.getCodeCompletionPolicy() == CodeCompletionPolicy.OFF)
        {
            settingsSetter.setCodeCompletionPolicy(CodeCompletionPolicy.MODERATE);
        }

        stateService.setState(this.getClass().getName(), ServiceState.SETTINGS_CHANGED);
        return result;
    }

    private static Image createImage(String path)
    {
        var descriptor = ImageDescriptor
            .createFromURL(FileLocator.find(BaseActivator.getDefault().getBundle(), new Path(path), null));
        return descriptor.createImage();
    }

    private static class PolicyComboFieldEditor
        extends ComboFieldEditor
    {
        private static final String[][] CODE_COMPLETION_POLICIES;
        private Combo combo;

        static
        {
            CODE_COMPLETION_POLICIES = new String[CodeCompletionPolicy.values().length][];
            var index = 0;
            for (var policy : CodeCompletionPolicy.values())
            {
                CODE_COMPLETION_POLICIES[index++] = new String[] { policy.getLongName(), policy.getId() };
            }
        }

        public PolicyComboFieldEditor(Composite parent)
        {
            super(ISettingsStore.CODE_COMPLETION_POLICY, Messages.ClientAIPreferencePage_CodeCompletionPolicy,
                CODE_COMPLETION_POLICIES, parent);
        }

        @Override
        protected void doFillIntoGrid(Composite parent, int numColumns)
        {
            super.doFillIntoGrid(parent, numColumns);
            var childernAfter = parent.getChildren();
            if (childernAfter.length > 0)
            {
                var control = childernAfter[childernAfter.length - 1];
                if (control instanceof Combo)
                {
                    combo = (Combo)control;
                }
            }
        }

        @Override
        protected void doLoad()
        {
            super.doLoad();
            updateToolTipText();
        }

        @Override
        protected void doLoadDefault()
        {
            super.doLoadDefault();
            updateToolTipText();
        }

        @Override
        protected void valueChanged(String oldValue, String newValue)
        {
            super.valueChanged(oldValue, newValue);
            updateToolTipText();
        }

        private void updateToolTipText()
        {
            if (combo == null)
            {
                return;
            }

            var index = combo.getSelectionIndex();
            if (index >= 0 && index < CodeCompletionPolicy.values().length)
            {
                combo.setToolTipText(CodeCompletionPolicy.values()[index].getDescription());
            }
            else
            {
                combo.setToolTipText(""); //$NON-NLS-1$
            }
        }
    }
}
