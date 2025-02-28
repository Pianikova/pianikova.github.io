/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import com.e1c.edt.ai.context.DTO.Comment;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslDocumentationComment;

interface ICommentFactory
{
    Comment create(BslDocumentationComment bslComment);
}
