/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.client.model;

import com.google.gson.annotations.SerializedName;

/**
 * This class contains information about response-JSON fields.
 * It uses as Serialized-from-JSON object. It keeps response data
 * after request with setting chat parameters
 * @author Bogdan Sushkov
 */
public class FeedbackResponse
{
    private Errors errors;
    private String message;

    /**
     * Contstructor of <code>FeedbackResponse</code> object. Takes <code><b>String</b> response</code> as parameter.
     * If request feedback was sucessful, message <code>"ok"</code> will set. Otherwise object will contain
     * error message.
     * @param response
     */
    public FeedbackResponse(String response)
    {
        message = response;
        errors = null;
    }

    /**
     * Returns <code>errors</code> parameter.
     * @return the errors
     */
    public Errors getErrors()
    {
        return errors;
    }

    /**
     * Sets <code>errors</code> parameter.
     * @param errors the errors to set
     */
    public void setErrors(Errors errors)
    {
        this.errors = errors;
    }

    /**
     * Rerurns <code>message</code> parameter.
     * @return the message
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Sets <code>message</code> parameter.
     * @param message the message to set
     */
    public void setMessage(String message)
    {
        this.message = message;
    }

    /**
     * This class contains information about error which can occure while making feedback request.
     * @author Bogdan Sushkov
     */
    public class Errors
    {
        @SerializedName("content.soft_score")
        private String softScore;
        @SerializedName("content.hard_score")
        private String hardScore;
        @SerializedName("content.extra_content")
        private String extraContent;

        /**
         * Returns message if softScore error occured or null otherwise.
         * @return softScore
         */
        public String getSoftScore()
        {
            return this.softScore;
        }

        /**
         * Returns message if hardScore error occured or null otherwise.
         * @return hardScore
         */
        public String hatHardScore()
        {
            return this.hardScore;
        }

        /**
         * Returns message if extraContent error occured or null otherwise.
         * @return extraContent
         */
        public String getExtraContent()
        {
            return this.extraContent;
        }

        /**
         * Sets <code>softScore</code> error message.
         * @param errorMessage
         */
        public void setSoftScore(String errorMessage)
        {
            this.softScore = errorMessage;
        }

        /**
         * Sets <code>hardScore</code> error message.
         * @param errorMessage
         */
        public void setHardScore(String errorMessage)
        {
            this.hardScore = errorMessage;
        }

        /**
         * Sets <code>extraContent</code> error message.
         * @param errorMessage
         */
        public void setExtraContent(String errorMessage)
        {
            this.extraContent = errorMessage;
        }
    }

}
