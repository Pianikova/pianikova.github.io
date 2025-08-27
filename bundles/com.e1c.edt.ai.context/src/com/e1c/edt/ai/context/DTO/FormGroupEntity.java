/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context.DTO;

import java.util.List;
import java.util.Map;

import org.eclipse.emf.ecore.EObject;

import com.google.gson.annotations.SerializedName;

public class FormGroupEntity
    extends ChildEntity
{
    public String kind;

    public Map<String, String> title;

    @SerializedName("tool_tip")
    public Map<String, String> toolTip;

    public List<FormFieldEntity> fields;

    public List<FormGroupEntity> groups;

    public List<FormButtonEntity> buttons;

    public transient EObject ref;
}
