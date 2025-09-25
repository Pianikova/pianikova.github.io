/**
 *
 */
package com.e1c.edt.ai.context;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.widgets.Form;
import org.eclipse.ui.forms.widgets.Section;

import com._1c.g5.lwt.AbstractLightControl;
import com._1c.g5.lwt.ILightComposite;
import com._1c.g5.lwt.controls.LightCheckbox;
import com._1c.g5.lwt.controls.LightEditorBar;
import com._1c.g5.lwt.controls.LightLabel;
import com._1c.g5.lwt.controls.LightText;
import com._1c.g5.lwt.interop.SwtLightComposite;
import com._1c.g5.lwt.interop.SwtLightControl;
import com.e1c.edt.ai.ICancellationToken;
import com.e1c.edt.ai.IVisualContextProvider;
import com.e1c.edt.ai.assistent.model.VisualContext;
import com.e1c.edt.ai.assistent.model.VisualField;
import com.e1c.edt.ai.assistent.model.VisualGroup;

public class VisualContextProvider implements IVisualContextProvider
{
    @Override
    public VisualContext create(Object controlObject, ICancellationToken cancellationToken)
    {
        var ctx = new VisualContext();
        if (!(controlObject instanceof Control))
        {
            return ctx;
        }

        var control = (Control)controlObject;
        var root = control.getParent();
        var rootGroup = new VisualGroup();
        rootGroup.fields = new ArrayList<>();
        while (root != null && !cancellationToken.isCanceled())
        {
            if (root instanceof Form)
            {
                rootGroup.title = ((Form)root).getText();
                break;
            }

            if (root instanceof Section)
            {
                rootGroup.title = ((Section)root).getText();
            }

            root = root.getParent();
        }

        var target = SwtLightComposite.getSwtLightControl(control);
        if (target == null)
        {
            return ctx;
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

        if (cancellationToken.isCanceled())
        {
            return ctx;
        }

        var groups = new ArrayList<VisualGroup>();
        findElements(parent, rootGroup, groups, cancellationToken);

        if (rootGroup.title != null && !rootGroup.title.isBlank())
        {
            ctx.title = rootGroup.title;
        }

        if (!rootGroup.fields.isEmpty())
        {
            ctx.fields = rootGroup.fields;
        }

        if (!groups.isEmpty())
        {
            ctx.groups = groups;
        }

        return ctx;
    }

    private void findElements(ILightComposite composite, VisualGroup currentGroup,
        List<VisualGroup> groups, ICancellationToken cancellationToken)
    {
        var visualField = new VisualField();
        for (var child : composite.getChildren())
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            if (child instanceof LightLabel)
            {
                var item = (LightLabel)child;
                visualField.isFocused = isFocused(item);
                visualField.name = item.getText();
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
                    visualField.isFocused = isFocused(item);
                    visualField.isMultiline = item.isMultiline();
                    visualField.value = item.getText();
                    currentGroup.fields.add(visualField);
                    visualField = new VisualField();
                }

                continue;
            }

            if (child instanceof LightText)
            {
                var item = (LightText)child;
                visualField.isFocused = isFocused(item);
                visualField.isMultiline = item.isMultiline();
                visualField.value = item.getText();
                currentGroup.fields.add(visualField);
                visualField = new VisualField();
                continue;
            }

            if (child instanceof LightCheckbox)
            {
                var item = (LightCheckbox)child;
                visualField.isFocused = isFocused(item);
                visualField.isMultiline = false;
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
                findElements(item, currentGroup, groups, cancellationToken);
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
