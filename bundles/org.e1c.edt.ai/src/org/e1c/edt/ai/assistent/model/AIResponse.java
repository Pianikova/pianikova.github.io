/**
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

import com.google.gson.annotations.SerializedName;

public class AIResponse
{
    private int index;
    private Token token;
    @SerializedName("generated_text")
    private String generatedText;
    private Object details;

    public int getIndex()
    {
        return index;
    }

    public void setIndex(int index)
    {
        this.index = index;
    }

    public Token getToken()
    {
        return token;
    }

    public void setToken(Token token)
    {
        this.token = token;
    }

    public String getGeneratedText()
    {
        return generatedText;
    }

    public void setGeneratedText(String generatedText)
    {
        this.generatedText = generatedText;
    }

    public Object getDetails()
    {
        return details;
    }

    public void setDetails(Object details)
    {
        this.details = details;
    }

    public static class Token
    {
        private int id;
        private String text;
        private double logprob;
        private boolean special;

        public int getId()
        {
            return id;
        }

        public void setId(int id)
        {
            this.id = id;
        }

        public String getText()
        {
            return text;
        }

        public void setText(String text)
        {
            this.text = text;
        }

        public double getLogprob()
        {
            return logprob;
        }

        public void setLogprob(double logprob)
        {
            this.logprob = logprob;
        }

        public boolean isSpecial()
        {
            return special;
        }

        public void setSpecial(boolean special)
        {
            this.special = special;
        }
    }
}
