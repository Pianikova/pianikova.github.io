/**
 * Copyright (C) 2024, 1C
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

public interface IFormVisitor
{
    void visitFormField(Optional<EObject> parent, FormField field);

    void visitField(Optional<EObject> parent, Field field);

    void visitButton(Optional<EObject> parent, Button button);

    void visitTable(Optional<EObject> parent, Table table);

    void visitAddition(Optional<EObject> parent, Addition addition);

    void visitDecoration(Optional<EObject> parent, Decoration decoration);

    void visitForm(Optional<EObject> parent, Form form);

    void visitGroup(Optional<EObject> parent, Group group);

    void visitFormItem(Optional<EObject> parent, FormItem formItem);

    void visitEObject(Optional<EObject> parent, EObject eObject);
}
