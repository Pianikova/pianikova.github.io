/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.HashSet;
import java.util.Optional;
import java.util.Stack;

import com.e1c.edt.ai.ICancellationToken;
import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.form.model.Addition;
import com._1c.g5.v8.dt.form.model.Button;
import com._1c.g5.v8.dt.form.model.CommandBarHolder;
import com._1c.g5.v8.dt.form.model.ContextMenuHolder;
import com._1c.g5.v8.dt.form.model.Decoration;
import com._1c.g5.v8.dt.form.model.ExtendedTooltipHolder;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.FormItem;
import com._1c.g5.v8.dt.form.model.FormItemContainer;
import com._1c.g5.v8.dt.form.model.Group;
import com._1c.g5.v8.dt.form.model.Table;
import com._1c.g5.v8.dt.form.model.TableHolder;
import com._1c.g5.v8.dt.mcore.Field;
import com._1c.g5.v8.dt.mcore.FieldSource;
import com.google.common.base.Preconditions;

class FormWalker
    implements IFormWalker
{
    @Override
    public void walk(EObject root, IFormVisitor visitor, ICancellationToken cancellationToken)
    {
        Preconditions.checkNotNull(root);
        Preconditions.checkNotNull(visitor);
        var items = new HashSet<EObject>();
        var stack = new Stack<EObject>();
        visit(stack, null, root, visitor, items);
        stack.push(root);
        while(stack.size() > 0)
        {
            if (cancellationToken.isCanceled())
            {
                break;
            }

            var parent = stack.pop();
            if (parent instanceof FormItemContainer)
            {
                var container = (FormItemContainer)parent;
                for (var item : container.getItems())
                {
                    visit(stack, parent, item, visitor, items);
                }
            }

            if (parent instanceof FieldSource)
            {
                var fieldSource = (FieldSource)parent;
                for (var item : fieldSource.getFields())
                {
                    visit(stack, parent, item, visitor, items);
                }
            }

            if (parent instanceof CommandBarHolder)
            {
                var holder = (CommandBarHolder)parent;
                visit(stack, parent, holder.getAutoCommandBar(), visitor, items);
            }

            if (parent instanceof ContextMenuHolder)
            {
                var holder = (ContextMenuHolder)parent;
                visit(stack, parent, holder.getContextMenu(), visitor, items);
            }

            if (parent instanceof ExtendedTooltipHolder)
            {
                var holder = (ExtendedTooltipHolder)parent;
                visit(stack, parent, holder.getExtendedTooltip(), visitor, items);
            }

            if (parent instanceof TableHolder)
            {
                var holder = (TableHolder)parent;
                visit(stack, parent, holder.getAutoTable(), visitor, items);
            }

            if (parent instanceof Table)
            {
                var table = (Table)parent;
                for (var item : table.getItems())
                {
                    visit(stack, table, item, visitor, items);
                }
            }
        }
    }

    private void visit(final Stack<EObject> stack, final EObject parent, final EObject item, final IFormVisitor visitor,
        HashSet<EObject> items)
    {
        if (item == null)
        {
            return;
        }

        if (!items.add(item))
        {
            return;
        }

        stack.push(item);

        if (item instanceof FormField)
        {
            var field = (FormField)item;
            visitor.visitFormField(Optional.ofNullable(parent), field);
        }

        if (item instanceof Field)
        {
            var field = (Field)item;
            visitor.visitField(Optional.ofNullable(parent), field);
        }

        if (item instanceof Button)
        {
            var button = (Button)item;
            visitor.visitButton(Optional.ofNullable(parent), button);
        }

        if (item instanceof Table)
        {
            var table = (Table)item;
            visitor.visitTable(Optional.ofNullable(parent), table);
        }

        if (item instanceof Addition)
        {
            var addition = (Addition)item;
            visitor.visitAddition(Optional.ofNullable(parent), addition);
        }

        if (item instanceof Decoration)
        {
            var decoration = (Decoration)item;
            visitor.visitDecoration(Optional.ofNullable(parent), decoration);
        }

        if (item instanceof Form)
        {
            var form = (Form)item;
            visitor.visitForm(Optional.ofNullable(parent), form);
        }

        if (item instanceof Group)
        {
            var group = (Group)item;
            visitor.visitGroup(Optional.ofNullable(parent), group);
        }

        if (item instanceof FormItem)
        {
            var formItem = (FormItem)item;
            visitor.visitFormItem(Optional.ofNullable(parent), formItem);
        }

        if (item != null)
        {
            visitor.visitEObject(Optional.ofNullable(parent), item);
        }
    }
}
