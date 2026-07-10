/**
 *
 */
package com.e1c.edt.ai.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;

import com._1c.g5.lwt.AbstractLightControl;
import com._1c.g5.lwt.ILightComposite;
import com._1c.g5.lwt.controls.LightCheckbox;
import com._1c.g5.lwt.controls.LightEditorBar;
import com._1c.g5.lwt.controls.LightLabel;
import com._1c.g5.lwt.controls.LightText;
import com._1c.g5.lwt.interop.SwtLightComposite;
import com._1c.g5.lwt.interop.SwtLightControl;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.ILog;
import com.e1c.edt.ai.assistent.model.VisualField;
import com.e1c.edt.ai.assistent.model.VisualGroup;
import com.e1c.edt.ai.ui.IClipboard;
import com.e1c.edt.ai.ui.IDispatcher;
import com.e1c.edt.ai.ui.IUI;
import com.e1c.edt.ai.ui.SwtVisualContextProvider;
import com.google.inject.Inject;

/**
 * EDT variant of the visual context provider: extends the generic SWT walker with support
 * for the 1C Light Widget Toolkit (LWT) controls used by EDT metadata editors.
 */
public class VisualContextProvider
    extends SwtVisualContextProvider
{
    @Inject
    public VisualContextProvider(IDispatcher dispatcher, IClipboard clipboard, IUI ui, ILog log)
    {
        super(dispatcher, clipboard, ui, log);
    }

    @Override
    protected void collectFrom(Control control, Composite root, VisualGroup rootGroup, List<VisualGroup> groups,
        CaptureBudget budget, ICancellationToken cancellationToken)
    {
        var target = SwtLightComposite.getSwtLightControl(control);
        if (target == null)
        {
            super.collectFrom(control, root, rootGroup, groups, budget, cancellationToken);
            return;
        }

        var parent = target.getParent();
        while (!cancellationToken.isCanceled())
        {
            var nextParent = parent.getParent();
            if (nextParent == null)
            {
                break;
            }

            parent = nextParent;
        }

        findElements(parent, rootGroup, groups, budget, cancellationToken);
    }

    @Override
    protected boolean visitCustomChild(Control child, VisualGroup currentGroup, List<VisualGroup> groups,
        CaptureBudget budget, ICancellationToken cancellationToken)
    {
        var light = SwtLightComposite.getSwtLightControl(child);
        if (light instanceof ILightComposite)
        {
            findElements((ILightComposite)light, currentGroup, groups, budget, cancellationToken);
            return true;
        }

        return false;
    }

    private void findElements(ILightComposite composite, VisualGroup currentGroup,
        List<VisualGroup> groups, CaptureBudget budget, ICancellationToken cancellationToken)
    {
        var visualField = new VisualField();
        for (var child : composite.getChildren())
        {
            if (cancellationToken.isCanceled() || budget.isExhausted())
            {
                break;
            }

            budget.onControl();
            if (child instanceof LightLabel)
            {
                var item = (LightLabel)child;
                visualField.isFocused = isFocused(item);
                var name = visualField.name;
                visualField.name = ((name == null ? "" : name + " ") + item.getText()).trim(); //$NON-NLS-1$ //$NON-NLS-2$
                continue;
            }

            if (child instanceof LightEditorBar)
            {
                @SuppressWarnings("rawtypes")
                var editorBar = (LightEditorBar)child;
                var content = editorBar.getContent();
                if (content instanceof LightText)
                {
                    var item = (LightText)content;
                    visualField.kind = "text"; //$NON-NLS-1$
                    visualField.isFocused = isFocused(item);
                    visualField.isMultiline = item.isMultiline();
                    visualField.value = truncate(item.getText(), visualField, budget.getMaxValueLength());
                    currentGroup.fields.add(visualField);
                    visualField = new VisualField();
                }

                continue;
            }

            if (child instanceof LightText)
            {
                var item = (LightText)child;
                visualField.kind = "text"; //$NON-NLS-1$
                visualField.isFocused = isFocused(item);
                visualField.isMultiline = item.isMultiline();
                visualField.value = truncate(item.getText(), visualField, budget.getMaxValueLength());
                currentGroup.fields.add(visualField);
                visualField = new VisualField();
                continue;
            }

            if (child instanceof LightCheckbox)
            {
                var item = (LightCheckbox)child;
                visualField.kind = "checkbox"; //$NON-NLS-1$
                visualField.isFocused = isFocused(item);
                visualField.isMultiline = false;
                visualField.isChecked = item.isChecked();
                visualField.value = item.isChecked() ? "[X]" : "[ ]"; //$NON-NLS-1$//$NON-NLS-2$
                currentGroup.fields.add(visualField);
                visualField = new VisualField();
                continue;
            }

            if (child instanceof SwtLightControl)
            {
                var lightControl = (SwtLightControl)child;
                var swtControl = lightControl.getSwtControl();
                if (swtControl instanceof Label)
                {
                    var item = (Label)swtControl;
                    currentGroup = new VisualGroup();
                    currentGroup.title = item.getText();
                    currentGroup.fields = new ArrayList<>();
                    groups.add(currentGroup);
                }
            }

            if (child instanceof SwtLightComposite)
            {
                var item = (SwtLightComposite)child;
                findElements(item, currentGroup, groups, budget, cancellationToken);
                continue;
            }
        }
    }

    private boolean isFocused(AbstractLightControl control)
    {
        if (control.isFocused())
        {
            return true;
        }

        return Optional.ofNullable(control.getOverlay())
            .map(i -> i.getSwtControl())
            .map(i -> i.isFocusControl())
            .orElse(false);
    }
}
