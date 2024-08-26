/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.context;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslDocumentationComment;

public interface ICommentFactory
{
    Comment create(BslDocumentationComment bslComment);
}
