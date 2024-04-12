/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

import java.util.List;

import com.google.gson.annotations.SerializedName;

/**
 * @author Bogdan Sushkov
 *
 */
public class AITextRequest
{
    private String inputs;
    private Parameters parameters;

    /**
     * @return the inputs
     */
    public String getInputs()
    {
        return inputs;
    }

    /**
     * @param inputs the inputs to set
     */
    public void setInputs(String inputs)
    {
        this.inputs = inputs;
    }

    /**
     * @return the parameters
     */
    public Parameters getParameters()
    {
        return parameters;
    }

    /**
     * @param parameters the parameters to set
     */
    public void setParameters(Parameters parameters)
    {
        this.parameters = parameters;
    }

    public class Parameters
    {
        @SerializedName("best_of")
        private int bestOf = 1;
        @SerializedName("decoder_input_details")
        private boolean decoderInputDetails = false;
        private boolean details = false;
        @SerializedName("do_sample")
        private boolean doSample = true;
        @SerializedName("max_new_tokens")
        private int maxNewTokens = 120;
        @SerializedName("repetition_penalty")
        private double repetitionPenalty = 1.03;
        @SerializedName("frequency_penalty")
        private double frequencyPenalty = 0.1;
        @SerializedName("return_full_text")
        private boolean returnFullText;
        private Boolean seed;
        private List<String> stop;
        private double temperature = 0.2;
        @SerializedName("top_k")
        private int topK = 10;
        @SerializedName("top_n_tokens")
        private int topNTokens = 5;
        @SerializedName("top_p")
        private double topP = 0.95;
        private Boolean truncate;
        @SerializedName("typical_p")
        private double typicalP = 0.95;
        private boolean watermark = true;
        /**
         * @return the bestOf
         */
        public int getBestOf()
        {
            return bestOf;
        }
        /**
         * @param bestOf the bestOf to set
         */
        public void setBestOf(int bestOf)
        {
            this.bestOf = bestOf;
        }

//        /**
//         * @return the decoderInputDetails
//         */
//        public boolean isDecoderInputDetails()
//        {
//            return decoderInputDetails;
//        }
//        /**
//         * @param decoderInputDetails the decoderInputDetails to set
//         */
//        public void setDecoderInputDetails(boolean decoderInputDetails)
//        {
//            this.decoderInputDetails = decoderInputDetails;
//        }
//        /**
//         * @return the details
//         */
//        public boolean isDetails()
//        {
//            return details;
//        }
//        /**
//         * @param details the details to set
//         */
//        public void setDetails(boolean details)
//        {
//            this.details = details;
//        }
//        /**
//         * @return the doSample
//         */
//        public boolean isDoSample()
//        {
//            return doSample;
//        }
//        /**
//         * @param doSample the doSample to set
//         */
//        public void setDoSample(boolean doSample)
//        {
//            this.doSample = doSample;
//        }
        /**
         * @return the maxNewTokens
         */
        public int getMaxNewTokens()
        {
            return maxNewTokens;
        }
        /**
         * @param maxNewTokens the maxNewTokens to set
         */
        public void setMaxNewTokens(int maxNewTokens)
        {
            this.maxNewTokens = maxNewTokens;
        }

//        /**
//         * @return the repetitionPenalty
//         */
//        public float getRepetitionPenalty()
//        {
//            return repetitionPenalty;
//        }
        /**
         * @param repetitionPenalty the repetitionPenalty to set
         */
        public void setRepetitionPenalty(float repetitionPenalty)
        {
            this.repetitionPenalty = repetitionPenalty;
        }

//        /**
//         * @return the returnFullText
//         */
//        public boolean isReturnFullText()
//        {
//            return returnFullText;
//        }
//        /**
//         * @param returnFullText the returnFullText to set
//         */
//        public void setReturnFullText(boolean returnFullText)
//        {
//            this.returnFullText = returnFullText;
//        }
//        /**
//         * @return the seed
//         */
//        public int getSeed()
//        {
//            return seed;
//        }
//        /**
//         * @param seed the seed to set
//         */
//        public void setSeed(int seed)
//        {
//            this.seed = seed;
//        }
        /**
         * @return the stop
         */
        public List<String> getStop()
        {
            return stop;
        }
        /**
         * @param stop the stop to set
         */
        public void setStop(List<String> stop)
        {
            this.stop = stop;
        }
//        /**
//         * @return the temperature
//         */
//        public float getTemperature()
//        {
//            return temperature;
//        }
//        /**
//         * @param temperature the temperature to set
//         */
//        public void setTemperature(float temperature)
//        {
//            this.temperature = temperature;
//        }
//        /**
//         * @return the topK
//         */
//        public int getTopK()
//        {
//            return topK;
//        }
//        /**
//         * @param topK the topK to set
//         */
//        public void setTopK(int topK)
//        {
//            this.topK = topK;
//        }
//        /**
//         * @return the topNTokens
//         */
//        public int getTopNTokens()
//        {
//            return topNTokens;
//        }
//        /**
//         * @param topNTokens the topNTokens to set
//         */
//        public void setTopNTokens(int topNTokens)
//        {
//            this.topNTokens = topNTokens;
//        }
//        /**
//         * @return the topP
//         */
//        public float getTopP()
//        {
//            return topP;
//        }
//        /**
//         * @param topP the topP to set
//         */
//        public void setTopP(float topP)
//        {
//            this.topP = topP;
//        }
//        /**
//         * @return the truncate
//         */
//        public int getTruncate()
//        {
//            return truncate;
//        }
//        /**
//         * @param truncate the truncate to set
//         */
//        public void setTruncate(int truncate)
//        {
//            this.truncate = truncate;
//        }
//        /**
//         * @return the typicalP
//         */
//        public float getTypicalP()
//        {
//            return typicalP;
//        }
//        /**
//         * @param typicalP the typicalP to set
//         */
//        public void setTypicalP(float typicalP)
//        {
//            this.typicalP = typicalP;
//        }
//        /**
//         * @return the watermark
//         */
//        public boolean isWatermark()
//        {
//            return watermark;
//        }
//        /**
//         * @param watermark the watermark to set
//         */
//        public void setWatermark(boolean watermark)
//        {
//            this.watermark = watermark;
//        }

    }
}
