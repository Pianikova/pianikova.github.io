/**
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.context;

import java.util.ArrayList;
import java.util.List;

import com.e1c.edt.ai.context.DTO.Comment;
import com.e1c.edt.ai.context.DTO.CommentDescriptionPart;
import com.e1c.edt.ai.context.DTO.CommentFieldDefinition;
import com.e1c.edt.ai.context.DTO.CommentParameter;
import com.e1c.edt.ai.context.DTO.CommentParameters;
import com.e1c.edt.ai.context.DTO.CommentReturn;
import com.e1c.edt.ai.context.DTO.CommentType;
import com.e1c.edt.ai.context.DTO.CommentTypeDefinition;

import com._1c.g5.v8.dt.bsl.documentation.comment.BslDocumentationComment;
import com._1c.g5.v8.dt.bsl.documentation.comment.BslDocumentationComment.Description;
import com._1c.g5.v8.dt.bsl.documentation.comment.BslDocumentationComment.ParametersSection;
import com._1c.g5.v8.dt.bsl.documentation.comment.BslDocumentationComment.ReturnSection;
import com._1c.g5.v8.dt.bsl.documentation.comment.LinkPart;
import com._1c.g5.v8.dt.bsl.documentation.comment.TextPart;
import com._1c.g5.v8.dt.bsl.documentation.comment.TypeSection;
import com._1c.g5.v8.dt.bsl.documentation.comment.TypeSection.FieldDefinition;
import com._1c.g5.v8.dt.bsl.documentation.comment.TypeSection.LinkContainsTypeDefinition;
import com._1c.g5.v8.dt.bsl.documentation.comment.TypeSection.TypeDefinition;

class CommentFactory
    implements ICommentFactory
{
    @Override
    public Comment create(BslDocumentationComment bslComment)
    {
        var comment = new Comment();
        comment.description = createDescription(bslComment.getDescription());

        comment.parameters = createParameters(bslComment.getParametersSection());

        var exampleSection = bslComment.getExampleSection();
        if (exampleSection != null)
        {
            comment.exampleDescription = createDescription(exampleSection.getDescription());
        }

        var callOptionsSection = bslComment.getCallOptionsSection();
        if (callOptionsSection != null)
        {
            comment.callOptionsDescription = createDescription(callOptionsSection.getDescription());
        }

        comment.returnInfo = createReturn(bslComment.getReturnSection());
        return comment;
    }

    private CommentReturn createReturn(ReturnSection returnSection)
    {
        if (returnSection == null)
        {
            return null;
        }

        var returnInfo = new CommentReturn();
        returnInfo.returnDescription = createDescription(returnSection.getDescription());
        var returnTypes = returnSection.getReturnTypes();
        if (!returnTypes.isEmpty())
        {
            returnInfo.returnTypes = new ArrayList<>();
            for (var returnType : returnSection.getReturnTypes())
            {
                returnInfo.returnTypes.add(createType(returnType));
            }
        }

        return returnInfo;
    }

    private CommentParameters createParameters(ParametersSection parametersSection)
    {
        if (parametersSection == null)
        {
            return null;
        }

        var commentParameters = new CommentParameters();
        var params = parametersSection.getParameterDefinitions();
        if (!params.isEmpty())
        {
            commentParameters.parameters = new ArrayList<>();
            for (var param : params)
            {
                var commentParameter = new CommentParameter();
                commentParameters.parameters.add(commentParameter);
                commentParameter.description = createDescription(param.getDescription());
                commentParameter.name = param.getName();
                var types = param.getTypeSections();
                if (!types.isEmpty())
                {
                    commentParameter.types = new ArrayList<>();
                    for (var type : types)
                    {
                        commentParameter.types.add(createType(type));
                    }
                }
            }
        }

        commentParameters.parametersDescription = createDescription(parametersSection.getDescription());
        commentParameters.sourceDescription = createDescription(parametersSection.getSourceDescription());
        var parameterFieldDefinitions = parametersSection.getParameterDefinitions();
        if (!parameterFieldDefinitions.isEmpty())
        {
            commentParameters.parametersFieldDefinitions = new ArrayList<>();
            for (var parameterFieldDefinition : parameterFieldDefinitions)
            {
                commentParameters.parametersFieldDefinitions.add(createFieldDefinition(parameterFieldDefinition));
            }
        }

        return commentParameters;
    }

    private CommentType createType(TypeSection type)
    {
        var commentType = new CommentType();
        commentType.description = createDescription(type.getDescription());
        commentType.sourceDescription = createDescription(type.getSourceDescription());
        commentType.sourceExtensionDescription = createDescription(type.getSourceExtensionDescription());
        var typeDefinitions = type.getTypeDefinitions();
        if (!typeDefinitions.isEmpty())
        {
            commentType.typeDefinitions = new ArrayList<>();
            for (var typeDefinition : typeDefinitions)
            {
                commentType.typeDefinitions.add(createTypeDefenition(typeDefinition));
            }
        }

        return commentType;
    }

    private CommentTypeDefinition createTypeDefenition(TypeDefinition typeDefinition)
    {
        var commentTypeDefinition = new CommentTypeDefinition();
        commentTypeDefinition.name = typeDefinition.getTypeName();
        var fieldDefenitions = typeDefinition.getFieldDefinitionExtension();
        if (!fieldDefenitions.isEmpty())
        {
            commentTypeDefinition.fieldDefinitions = new ArrayList<>();
            for (var fieldDefinition : fieldDefenitions)
            {
                commentTypeDefinition.fieldDefinitions.add(createFieldDefinition(fieldDefinition));
            }
        }

        return commentTypeDefinition;
    }

    private CommentFieldDefinition createFieldDefinition(FieldDefinition fieldDefinition)
    {
        var commentFieldDefinition = new CommentFieldDefinition();
        commentFieldDefinition.name = fieldDefinition.getName();
        commentFieldDefinition.description = createDescription(fieldDefinition.getDescription());
        var fieldDefenitions = fieldDefinition.getTypeSections();
        if (!fieldDefenitions.isEmpty())
        {
            commentFieldDefinition.types = new ArrayList<>();
            for (var fieldType : fieldDefenitions)
            {
                commentFieldDefinition.types.add(createType(fieldType));
            }
        }

        return commentFieldDefinition;
    }

    private List<CommentDescriptionPart> createDescription(Description description)
    {
        if (description == null)
        {
            return null;
        }

        var parts = description.getParts();
        if (parts.isEmpty())
        {
            return null;
        }

        var descrition = new ArrayList<CommentDescriptionPart>();
        for (var part : parts)
        {
            var descriptionPart = new CommentDescriptionPart();
            descrition.add(descriptionPart);
            if (part instanceof TextPart)
            {
                var textPart = (TextPart)part;
                descriptionPart.kind = "text"; //$NON-NLS-1$
                descriptionPart.text = textPart.getText();
                continue;
            }

            if (part instanceof LinkPart)
            {
                var linkPart = (LinkPart)part;
                descriptionPart.kind = "link"; //$NON-NLS-1$
                descriptionPart.text = linkPart.getInitialContent();
                descriptionPart.link = linkPart.getLinkText();
                continue;
            }

            if (part instanceof TypeSection)
            {
                var typeSection = (TypeSection)part;
                descriptionPart.kind = "type"; //$NON-NLS-1$
                descriptionPart.type = createType(typeSection);
                continue;
            }

            if (part instanceof ParametersSection)
            {
                var parametersSection = (ParametersSection)part;
                descriptionPart.kind = "parameters"; //$NON-NLS-1$
                descriptionPart.parameters = createParameters(parametersSection);
                continue;
            }

            if (part instanceof ReturnSection)
            {
                var returnSection = (ReturnSection)part;
                descriptionPart.kind = "return"; //$NON-NLS-1$
                descriptionPart.returnInfo = createReturn(returnSection);
                continue;
            }

            if (part instanceof FieldDefinition)
            {
                var fieldDefinition = (FieldDefinition)part;
                descriptionPart.kind = "field"; //$NON-NLS-1$
                descriptionPart.field = createFieldDefinition(fieldDefinition);
                continue;
            }

            if (part instanceof LinkContainsTypeDefinition)
            {
                var linkContainsTypeDefinition = (LinkContainsTypeDefinition)part;
                descriptionPart.kind = "linkWithType"; //$NON-NLS-1$
                descriptionPart.link = linkContainsTypeDefinition.getLink().getLinkText();
                descriptionPart.linkToExtensionFields =
                    linkContainsTypeDefinition.getLinkToExtensionFields().getLinkText();
                descriptionPart.typeName = linkContainsTypeDefinition.getTypeName();
                var typeDefinitions = linkContainsTypeDefinition.getContainTypes();
                if (!typeDefinitions.isEmpty())
                {
                    descriptionPart.containingTypeDefinitions = new ArrayList<>();
                    for (var typeDefinition : typeDefinitions)
                    {
                        descriptionPart.containingTypeDefinitions.add(createTypeDefenition(typeDefinition));
                    }
                }

                var fieldDefinitions = linkContainsTypeDefinition.getFieldDefinitionExtension();
                if (!fieldDefinitions.isEmpty())
                {
                    descriptionPart.fieldDefinitions = new ArrayList<>();
                    for (var fieldDefinition : fieldDefinitions)
                    {
                        descriptionPart.fieldDefinitions.add(createFieldDefinition(fieldDefinition));
                    }
                }

                continue;
            }

            descriptionPart.kind = "unknown"; //$NON-NLS-1$
        }

        return descrition;
    }
}
