/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context.DTO;

import java.util.List;
import java.util.Map;

public class RegisterRecordEntity
{
    public String name;

    public String comment;

    public Map<String, String> synonym;

    public List<FieldEntity> fields;
}
