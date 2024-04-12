/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

import java.util.ArrayList;

import org.e1c.edt.ai.assistent.model.AITextResponse.Details.BestOfSequence.PrefillToken;
import org.e1c.edt.ai.assistent.model.AITextResponse.Details.BestOfSequence.Token;

import com.google.gson.annotations.SerializedName;

/**
 * @author Bogdan Sushkov
 *
 */
public class AITextResponse
{
    private Details details;
    @SerializedName("generated_text")
    private String generatedText;

    /**
     * @return the details
     */
    public Details getDetails()
    {
        return details;
    }

    /**
     * @param details the details to set
     */
    public void setDetails(Details details)
    {
        this.details = details;
    }

    /**
     * @return the generatedText
     */
    public String getGeneratedText()
    {
        return generatedText;
    }

    /**
     * @param generatedText the generatedText to set
     */
    public void setGeneratedText(String generatedText)
    {
        this.generatedText = generatedText;
    }

    public class Details
    {
        @SerializedName("best_of_sequences")
        private BestOfSequence bestOfSequence;
        @SerializedName("finish_reason")
        private String finishReason;
        @SerializedName("generated_tokens")
        private int generatedTokens;
        private ArrayList<PrefillToken> prefill;
        private int seed;
        private ArrayList<Token> tokens;
        @SerializedName("top_tokens")
        private ArrayList<ArrayList<Token>> topTokens;

        /**
         * @return the finishReason
         */
        public String getFinishReason()
        {
            return finishReason;
        }

        /**
         * @param finishReason the finishReason to set
         */
        public void setFinishReason(String finishReason)
        {
            this.finishReason = finishReason;
        }

        /**
         * @return the generatedTokens
         */
        public int getGeneratedTokens()
        {
            return generatedTokens;
        }

        /**
         * @param generatedTokens the generatedTokens to set
         */
        public void setGeneratedTokens(int generatedTokens)
        {
            this.generatedTokens = generatedTokens;
        }

        /**
         * @return the prefill
         */
        public ArrayList<PrefillToken> getPrefill()
        {
            return prefill;
        }

        /**
         * @param prefill the prefill to set
         */
        public void setPrefill(ArrayList<PrefillToken> prefill)
        {
            this.prefill = prefill;
        }

        /**
         * @return the seed
         */
        public int getSeed()
        {
            return seed;
        }

        /**
         * @param seed the seed to set
         */
        public void setSeed(int seed)
        {
            this.seed = seed;
        }

        /**
         * @return the tokens
         */
        public ArrayList<Token> getTokens()
        {
            return tokens;
        }

        /**
         * @param tokens the tokens to set
         */
        public void setTokens(ArrayList<Token> tokens)
        {
            this.tokens = tokens;
        }

        /**
         * @return the topTokens
         */
        public ArrayList<ArrayList<Token>> getTopTokens()
        {
            return topTokens;
        }

        /**
         * @param topTokens the topTokens to set
         */
        public void setTopTokens(ArrayList<ArrayList<Token>> topTokens)
        {
            this.topTokens = topTokens;
        }

        /**
         * @return the bestOfSequence
         */
        public BestOfSequence getBestOfSequence()
        {
            return bestOfSequence;
        }

        /**
         * @param bestOfSequence the bestOfSequence to set
         */
        public void setBestOfSequence(BestOfSequence bestOfSequence)
        {
            this.bestOfSequence = bestOfSequence;
        }

        public class BestOfSequence
        {
            @SerializedName("finish_reason")
            private String finishReason;
            @SerializedName("generated_text")
            private String generatedText;
            @SerializedName("generated_tokens")
            private int generatedTokens;
            private ArrayList<PrefillToken> prefill;
            private int seed;
            private ArrayList<Token> tokens;
            @SerializedName("top_tokens")
            private ArrayList<ArrayList<Token>> topTokens;

            /**
             * @return the finishReason
             */
            public String getFinishReason()
            {
                return finishReason;
            }

            /**
             * @param finishReason the finishReason to set
             */
            public void setFinishReason(String finishReason)
            {
                this.finishReason = finishReason;
            }

            /**
             * @return the generatedText
             */
            public String getGeneratedText()
            {
                return generatedText;
            }

            /**
             * @param generatedText the generatedText to set
             */
            public void setGeneratedText(String generatedText)
            {
                this.generatedText = generatedText;
            }

            /**
             * @return the generatedTokens
             */
            public int getGeneratedTokens()
            {
                return generatedTokens;
            }

            /**
             * @param generatedTokens the generatedTokens to set
             */
            public void setGeneratedTokens(int generatedTokens)
            {
                this.generatedTokens = generatedTokens;
            }

            /**
             * @return the prefill
             */
            public ArrayList<PrefillToken> getPrefill()
            {
                return prefill;
            }

            /**
             * @param prefill the prefill to set
             */
            public void setPrefill(ArrayList<PrefillToken> prefill)
            {
                this.prefill = prefill;
            }

            /**
             * @return the seed
             */
            public int getSeed()
            {
                return seed;
            }

            /**
             * @param seed the seed to set
             */
            public void setSeed(int seed)
            {
                this.seed = seed;
            }

            /**
             * @return the tokens
             */
            public ArrayList<Token> getTokens()
            {
                return tokens;
            }

            /**
             * @param tokens the tokens to set
             */
            public void setTokens(ArrayList<Token> tokens)
            {
                this.tokens = tokens;
            }

            /**
             * @return the topTokens
             */
            public ArrayList<ArrayList<Token>> getTopTokens()
            {
                return topTokens;
            }

            /**
             * @param topTokens the topTokens to set
             */
            public void setTopTokens(ArrayList<ArrayList<Token>> topTokens)
            {
                this.topTokens = topTokens;
            }

            public class Token
                extends PrefillToken
            {
                private boolean special;

                /**
                 * @return the special
                 */
                public boolean isSpecial()
                {
                    return special;
                }

                /**
                 * @param special the special to set
                 */
                public void setSpecial(boolean special)
                {
                    this.special = special;
                }

            }

            public class PrefillToken
            {
                private int id;
                private float logprob;
                private String text;

                /**
                 *
                 * TODO JavaDoc
                 *
                 * @param id
                 */
                public void setId(int id)
                {
                    this.id = id;
                }

                /**
                 *
                 * TODO JavaDoc
                 *
                 * @return
                 */
                public int getId()
                {
                    return this.id;
                }

                /**
                 *
                 * TODO JavaDoc
                 *
                 * @param logprob
                 */
                public void setLogprob(float logprob)
                {
                    this.logprob = logprob;
                }

                /**
                 *
                 * TODO JavaDoc
                 *
                 * @return
                 */
                public float getLogprob()
                {
                    return this.logprob;
                }

                /**
                 *
                 * TODO JavaDoc
                 *
                 * @param text
                 */
                public void setText(String text)
                {
                    this.text = text;
                }

                /**
                 *
                 * TODO JavaDoc
                 *
                 * @return
                 */
                public String getText()
                {
                    return this.text;
                }
            }
        }

    }
}
