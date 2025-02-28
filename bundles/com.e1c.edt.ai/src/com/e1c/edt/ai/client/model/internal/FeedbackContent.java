/*
 * Copyright (C) 2025, 1C
 */
package com.e1c.edt.ai.client.model.internal;

import com.google.gson.annotations.SerializedName;

/**
 * This class contains data to send in feedback request:
 * <blockquote><pre>
 * {
 *   softScore: String
 *   hardScore: String
 *   extraContent: ExtraContent
 * }
 * </pre></blockquote>
 * @author Bogdan Sushkov
 */
public class FeedbackContent
{
    @SerializedName("soft_score")
    private int softScore;
    @SerializedName("hard_score")
    private int hardScore;
    @SerializedName("extra_content")
    private ExtraContent extraContent;

    /**
     * Constructor of content without extra.
     * @param softScore
     * @param hardScore
     */
    public FeedbackContent(int softScore, int hardScore)
    {
        this.softScore = softScore;
        this.hardScore = hardScore;
        this.extraContent = new ExtraContent();
    }

    /**
     * Constructor of content, containing extra content.
     * @param softScore
     * @param hardScore
     */
    public FeedbackContent(int softScore, int hardScore, ExtraContent extraContent)
    {
        this.softScore = softScore;
        this.hardScore = hardScore;
        this.extraContent = extraContent;
    }

    /**
     * Returns <code>softScore</code> parameter.
     * @return the soft_score
     */
    public int getSoftScore()
    {
        return softScore;
    }

    /**
     * Sets <code>softScore</code> parameter.
     * @param soft_score the soft_score to set
     */
    public void setSoftScore(int soft_score)
    {
        this.softScore = soft_score;
    }

    /**
     * Returns <code>hardScore</code> parameter.
     * @return the hard_score
     */
    public int getHardScore()
    {
        return hardScore;
    }

    /**
     * Sets <code>hardScore</code> parameter.
     * @param hard_score the hard_score to set
     */
    public void setHardScore(int hard_score)
    {
        this.hardScore = hard_score;
    }

    /**
     * Returns <code>extraContent</code> parameter.
     * @return the extra_content
     */
    public ExtraContent getExtraContent()
    {
        return extraContent;
    }

    /**
     * Sets <code>extraContent</code> parameter.
     * @param extra_content the extra_content to set
     */
    public void setExtraContent(ExtraContent extra_content)
    {
        this.extraContent = extra_content;
    }

    /**
     * Marker class which provides storage for extra content of feedback.
     * @author Bogdan Sushkov
     */
    public class ExtraContent
    {
        private String extra;

        /**
         * Basic constructor of storage.
         */
        public ExtraContent()
        {
            extra = new String();
        }

        /**
         * Constructor  of storage from extra-message.
         */
        public ExtraContent(String param)
        {
            this.setExtra(param);
        }

        /**
         * Returns <code>extra</code> parameter.
         * @return the param
         */
        public String getParam()
        {
            return extra;
        }

        /**
         * Sets <code>extra</code> parameter.
         * @param param
         */
        public void setExtra(String param)
        {
            this.extra = param;
        }
    }

}