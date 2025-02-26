/**
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.context;

import org.e1c.edt.ai.context.DTO.Comment;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslDocumentationComment;

interface ICommentFactory
{
    Comment create(BslDocumentationComment bslComment);
}
