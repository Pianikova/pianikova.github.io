/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.context;

import java.util.Optional;

import org.eclipse.emf.ecore.EObject;

import com._1c.g5.v8.dt.form.model.Addition;
import com._1c.g5.v8.dt.form.model.Button;
import com._1c.g5.v8.dt.form.model.Decoration;
import com._1c.g5.v8.dt.form.model.Form;
import com._1c.g5.v8.dt.form.model.FormField;
import com._1c.g5.v8.dt.form.model.FormItem;
import com._1c.g5.v8.dt.form.model.Group;
import com._1c.g5.v8.dt.form.model.Table;
import com._1c.g5.v8.dt.mcore.Field;

class FormVisitor
    implements IFormVisitor
{
    @Override
    public void visitFormField(Optional<EObject> parent, FormField field)
    {
        //
    }

    @Override
    public void visitField(Optional<EObject> parent, Field field)
    {
        //
    }

    @Override
    public void visitButton(Optional<EObject> parent, Button button)
    {
        //
    }

    @Override
    public void visitTable(Optional<EObject> parent, Table table)
    {
        //
    }

    @Override
    public void visitAddition(Optional<EObject> parent, Addition addition)
    {
        //
    }

    @Override
    public void visitDecoration(Optional<EObject> parent, Decoration decoration)
    {
        //
    }

    @Override
    public void visitForm(Optional<EObject> parent, Form form)
    {
        //
    }

    @Override
    public void visitGroup(Optional<EObject> parent, Group group)
    {
        //
    }

    @Override
    public void visitFormItem(Optional<EObject> parent, FormItem formItem)
    {
        //
    }

    @Override
    public void visitEObject(Optional<EObject> parent, EObject eObject)
    {
        //
    }
}
