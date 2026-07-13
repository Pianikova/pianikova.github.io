/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.ui.preferences;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Objects;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.preference.BooleanFieldEditor;
import org.eclipse.jface.preference.ComboFieldEditor;
import org.eclipse.jface.preference.FieldEditor;
import org.eclipse.jface.preference.FieldEditorPreferencePage;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.IntegerFieldEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
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
import com.e1c.edt.ai.IStateService;
import com.e1c.edt.ai.IValidator;
import com.e1c.edt.ai.ServiceState;
import com.e1c.edt.ai.ValidationError;
import com.e1c.edt.ai.ValidationResult;
import com.e1c.edt.ai.WellknownError;
import com.e1c.edt.ai.assistent.model.CodeCompletionPolicy;
import com.e1c.edt.ai.ui.AIUICommonModule;
import com.e1c.edt.ai.ui.BaseActivator;
import com.e1c.edt.ai.ui.IWeb;
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

    // Column count of the page grid. Equals the largest field control count (the token editor:
    // label + text + validate button). Section groups reuse the same count so the base
    // FieldEditorPreferencePage#adjustGridLayout stays consistent for grouped editors.
    private static final int PAGE_COLUMNS = 3;

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
    @Inject
    IWeb web;

    private String prevToken;
    private TokenFieldEditor tokenFieldEditor;
    private boolean settingsChanged = false;

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

        // --- Top-level settings: access key ---
        tokenFieldEditor = new TokenFieldEditor(ISettingsStore.CLIENT_TOKEN,
            Messages.ClientAIPreferencePage_Client_Token, parent, new IValidator<String>()
            {
                @Override
                public ValidationResult validate(String token)
                {
                    // Empty or whitespace-only tokens are always valid
                    if (token == null || token.isBlank())
                    {
                        return ValidationResult.SUCCESS;
                    }

                    // Validate non-empty tokens using the client token validator
                    if (!clientTokenValidator.isValid(token))
                    {
                        return new ValidationResult(new ValidationError(WellknownError.InvalidToken, token));
                    }

                    return ValidationResult.SUCCESS;
                }
            });
        BaseActivator.injectMembers(tokenFieldEditor);
        setLabelTooltip(tokenFieldEditor, parent, Messages.ClientAIPreferencePage_Client_Token_Tooltip);
        var tokenText = tokenFieldEditor.getTextControl(parent);
        tokenText.setEchoChar('*');
        addField(tokenFieldEditor);

        // --- Group: User interface ---
        var userInterfaceGroup = createSectionGroup(parent, Messages.ClientAIPreferencePage_UserInterfaceGroup);

        var comboField = new ComboFieldEditor(ISettingsStore.LANGUAGE, Messages.ClientAIPreferencePage_Language,
            LANGUAGES, userInterfaceGroup);
        setLabelTooltip(comboField, userInterfaceGroup, Messages.ClientAIPreferencePage_Language_Tooltip);
        addField(comboField);

        var showStatusBarField = new BooleanFieldEditor(ISettingsStore.SHOW_STATUS_BAR,
            Messages.ClientAIPreferencePage_ShowStatusBar, userInterfaceGroup);
        addField(showStatusBarField);

        var showActivationInfoField = new BooleanFieldEditor(ISettingsStore.SHOW_ACTIVATION_INFO,
            Messages.ClientAIPreferencePage_ShowActivationInfo, userInterfaceGroup);
        addField(showActivationInfoField);

        finalizeSectionGroup(userInterfaceGroup);

        // --- Group: Code completion ---
        var codeCompletionGroup = createSectionGroup(parent, Messages.ClientAIPreferencePage_CodeCompletionGroup);

        var policyCombo = new PolicyComboFieldEditor(codeCompletionGroup);
        setLabelTooltip(policyCombo, codeCompletionGroup, Messages.ClientAIPreferencePage_CodeCompletionPolicy_Tooltip);
        addField(policyCombo);

        var codeCompletionLinesCount = new IntegerFieldEditor(ISettingsStore.CODE_COMPLETION_LINES_COUNT,
            Messages.ClientAIPreferencePage_CodeCompletionLinesCount, codeCompletionGroup);
        codeCompletionLinesCount.setValidRange(1, ISettingsStore.MAX_CODE_COMPLETION_LINES_COUNT);
        setLabelTooltip(codeCompletionLinesCount, codeCompletionGroup,
            Messages.ClientAIPreferencePage_CodeCompletionLinesCount_Tooltip);
        addField(codeCompletionLinesCount);

        finalizeSectionGroup(codeCompletionGroup);

        // --- Group: Chat ---
        var chatGroup = createSectionGroup(parent, Messages.ClientAIPreferencePage_ChatGroup);

        var autoOpenDiffPreviewField = new BooleanFieldEditor(ISettingsStore.AUTO_OPEN_DIFF_PREVIEW,
            Messages.ClientAIPreferencePage_AutoOpenDiffPreview, chatGroup);
        addField(autoOpenDiffPreviewField);

        finalizeSectionGroup(chatGroup);

        // --- Group: Additional features ---
        var advancedGroup = createSectionGroup(parent, Messages.ClientAIPreferencePage_AdvancedGroup);

        var backgroundAnalysisField = new BooleanFieldEditor(ISettingsStore.BACKGROUND_ANALYSIS,
            Messages.ClientAIPreferencePage_BackgroundAnalysis, advancedGroup);
        addField(backgroundAnalysisField);
        // The tooltip goes on the checkbox itself. Using setLabelTooltip here would call
        // getLabelControl on a default-style BooleanFieldEditor, which spawns a duplicate label.
        setCheckboxTooltip(advancedGroup, Messages.ClientAIPreferencePage_BackgroundAnalysis_Tooltip);

        finalizeSectionGroup(advancedGroup);

        // Parameters sits last so it renders right before the diagnostic section (added in createContents).
        var validatorField = new ValidatingStringFieldEditor(ISettingsStore.PARAMETERS,
            Messages.ClientAIPreferencePage_Parameters, parent, parametersValidator);
        setLabelTooltip(validatorField, parent, Messages.ClientAIPreferencePage_Parameters_Tooltip);
        addField(validatorField);
    }

    private void setLabelTooltip(FieldEditor editor, Composite parent, String tooltip)
    {
        var label = editor.getLabelControl(parent);
        label.setToolTipText(tooltip);
    }

    /**
     * Sets a tooltip on the checkbox of a {@link BooleanFieldEditor}. The editor keeps its label as
     * the checkbox text (no separate label control), so the tooltip is applied to the {@link Button}
     * directly instead of via {@code getLabelControl}, which would create a duplicate label.
     */
    private void setCheckboxTooltip(Composite group, String tooltip)
    {
        for (var child : group.getChildren())
        {
            if (child instanceof Button)
            {
                child.setToolTipText(tooltip);
            }
        }
    }

    /**
     * Creates a titled section group spanning the full page width, into which field editors are
     * placed by passing it as their parent.
     */
    private Group createSectionGroup(Composite parent, String text)
    {
        var group = new Group(parent, SWT.NONE);
        group.setText(text);
        var gd = new GridData(SWT.FILL, SWT.TOP, true, false);
        gd.horizontalSpan = PAGE_COLUMNS;
        group.setLayoutData(gd);
        group.setLayout(new GridLayout(PAGE_COLUMNS, false));
        return group;
    }

    /**
     * Restores the group's column count after its field editors were added. {@link FieldEditor}
     * resets the parent layout to its own control count on creation, so the page-wide column count
     * must be re-applied (and margins added) once all editors in the group exist.
     */
    private void finalizeSectionGroup(Group group)
    {
        var layout = (GridLayout)group.getLayout();
        layout.numColumns = PAGE_COLUMNS;
        layout.marginWidth = 10;
        layout.marginHeight = 8;
        layout.horizontalSpacing = 10;
    }

    @Override
    public void init(IWorkbench workbench)
    {
        this.prevToken = settings.getClientToken();
    }

    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        super.propertyChange(event);
        settingsChanged = true;
    }

    @SuppressWarnings("nls")
    @Override
    protected Control createContents(Composite parent)
    {
        var control = super.createContents(parent);

        // Host the diagnostic/branding sections inside the field-editor area (not as siblings of it).
        // The field-editor composite grabs excess vertical space; placing these as siblings would push
        // them to the bottom and leave a large empty gap above. Inside the grid they pack right after
        // the last group, and the slack falls below the version line instead.
        var extras = new Composite(getFieldEditorParent(), SWT.NONE);
        var extrasGd = new GridData(SWT.FILL, SWT.TOP, true, false);
        extrasGd.horizontalSpan = PAGE_COLUMNS;
        extras.setLayoutData(extrasGd);
        var extrasLayout = new GridLayout(1, false);
        extrasLayout.marginWidth = 0;
        extrasLayout.marginHeight = 0;
        extras.setLayout(extrasLayout);

        createDiagnosticSection(extras);

        var iconLabel = new Label(extras, SWT.NONE);
        iconLabel.setImage(SPLASH);

        var pluginLink = new Link(extras, SWT.NONE);
        pluginLink
            .setText("<a href=\"" + defaultSettings.getHomePage() + "\">" + defaultSettings.getHomePage() + "</a>");
        pluginLink.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                try
                {
                    web.browse(defaultSettings.getHomePage());
                }
                catch (Exception ex)
                {
                    log.logError("Failed to open URL: " + defaultSettings.getHomePage());
                    log.logError(ex);
                }
            }
        });

        var versionComposite = new Composite(extras, SWT.NONE);
        var versionLayout = new GridLayout(2, false);
        versionLayout.marginWidth = 0;
        versionLayout.marginHeight = 0;
        versionComposite.setLayout(versionLayout);

        var versionLabel = new Label(versionComposite, SWT.NONE);
        versionLabel.setText(MessageFormat.format(Messages.ClientAIPreferencePage_PluginVersion, getPluginVersion()));
        versionLabel.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));

        var copyVersionButton = new Button(versionComposite, SWT.PUSH);
        copyVersionButton.setText(Messages.ClientAIPreferencePage_CopyVersion);
        copyVersionButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        copyVersionButton.addSelectionListener(new SelectionAdapter()
        {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                var clipboard = new Clipboard(getShell().getDisplay());
                try
                {
                    clipboard.setContents(new Object[] { getPluginVersion() },
                        new Transfer[] { TextTransfer.getInstance() });
                }
                finally
                {
                    clipboard.dispose();
                }
            }
        });

        return control;
    }

    private static String getPluginVersion()
    {
        return BaseActivator.getDefault().getBundle().getVersion().toString();
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

        if (settingsChanged)
        {
            var token = settings.getClientToken();
            if (clientTokenValidator.isValid(token) && settings.getCodeCompletionPolicy() == CodeCompletionPolicy.OFF
                && !Objects.equals(token, this.prevToken))
            {
                settingsSetter.setCodeCompletionPolicy(CodeCompletionPolicy.MODERATE);
            }

            stateService.setState(ServiceState.SETTINGS_CHANGED);
        }

        this.prevToken = settings.getClientToken();
        return result;
    }

    /**
     * Creates section with plugin diagnostic on the preference page.
     *
     * @param parent
     */
    private void createDiagnosticSection(Composite parent)
    {
        Group diagnosticGroup = new Group(parent, SWT.NONE);
        diagnosticGroup.setText(Messages.ClientAIPreferencePage_Diagnostic);
        diagnosticGroup.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
        diagnosticGroup.setLayout(new GridLayout(2, false));

        GridLayout gl = (GridLayout)diagnosticGroup.getLayout();
        gl.marginHeight = 8;
        gl.marginWidth = 10;
        gl.horizontalSpacing = 10;

        Label info = new Label(diagnosticGroup, SWT.WRAP);
        info.setText(Messages.ClientAIPreferencePage_Diagnostic_Title);
        GridData infoGD = new GridData(SWT.FILL, SWT.TOP, true, false);
        infoGD.widthHint = 300;
        info.setLayoutData(infoGD);

        Button button = new Button(diagnosticGroup, SWT.PUSH);
        button.setText(Messages.ClientAIPreferencePage_Diagnostic_RunButton);
        button.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, false, false));
        button.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e)
            {
                var dialog = new DiagnosticDialog(getShell());
                dialog.open();
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e)
            {
                // skip
            }
        });
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
        private static final String[][] CODE_COMPLETION_POLICIES = Arrays.stream(CodeCompletionPolicy.values())
            .map(policy -> new String[] { policy.getLongName(), policy.getId() })
            .toArray(String[][]::new);
        private Combo combo;

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
            var policies = CodeCompletionPolicy.values();
            if (policies != null && policies.length > 0 && index >= 0 && index < policies.length)
            {
                combo.setToolTipText(policies[index].getDescription());
            }
            else
            {
                combo.setToolTipText(""); //$NON-NLS-1$
            }
        }
    }
}
