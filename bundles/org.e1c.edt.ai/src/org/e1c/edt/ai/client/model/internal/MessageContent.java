/*
 * Copyright (C) 2025, 1C
 */
package org.e1c.edt.ai.client.model.internal;

import java.util.ArrayList;

import com.google.gson.annotations.SerializedName;

/**
 * This class provides storage for data fields in message.
 * It usually stores message text and metainfo about sent response.
 * @author Bogdan Sushkov
 */
public class MessageContent
{
    private Data data;

    /**
     * Constructs MessageContent object with given data.
     * @param data
     * @param mode
     */
    public MessageContent(Data data)
    {
        this.data = data;
    }

    /**
     * Returns <code>data</code> parameter.
     * @return
     */
    public Data getData()
    {
        return this.data;
    }

    /**
     * Sets <code>data</code> parameter.
     * @param data
     */
    public void setData(Data data)
    {
        this.data = data;
    }

    /**
     * This class provides storage for message data
     * @author Bogdan Sushkov
     */
    public class Data
    {
        private String text;
        @SerializedName("documents")
        private ArrayList<String> documentsString;
        private ArrayList<Documents> documentsArray;

        /**
         * Constructor of Data. Takes <code>String message</code> and creates data for Message request/response.
         * @param message
         */
        public Data(String message)
        {
            this.text = message;
        }

        /**
         * Returns <code>text</code> parameter.
         * @return the text
         */
        public String getText()
        {
            return text;
        }

        /**
         * Returns <code>documents</code> parameter.
         * @return the documents
         */
        public ArrayList<Documents> getDocuments()
        {
            return documentsArray;
        }

        /**
         * Sets <code>text</code> parameter.
         * @param text
         */
        public void setText(String text)
        {
            this.text = text;
        }

        /**
         * Sets <code>documents</code> parameter.
         * @param documents
         */
        public void setDocuments(ArrayList<Documents> documents)
        {
            this.documentsArray = documents;
        }

        /**
         * @return the documentsString
         */
        public ArrayList<String> getDocumentsString()
        {
            return documentsString;
        }

        /**
         * @param documentsString the documentsString to set
         */
        public void setDocumentsString(ArrayList<String> documentsString)
        {
            this.documentsString = documentsString;
        }

        public class Documents
        {
            private String content;
            @SerializedName("chunk_index")
            private ArrayList<Integer> chunkIndex;
            private String query;
            private float score;
            private Metadata metadata;


            /**
             * @return the content
             */
            public String getContent()
            {
                return content;
            }

            /**
             * @param content the content to set
             */
            public void setContent(String content)
            {
                this.content = content;
            }

            /**
             * @return the chunkIndex
             */
            public ArrayList<Integer> getChunkIndex()
            {
                return chunkIndex;
            }

            /**
             * @param chunkIndex the chunkIndex to set
             */
            public void setChunkIndex(ArrayList<Integer> chunkIndex)
            {
                this.chunkIndex = chunkIndex;
            }

            /**
             * @return the query
             */
            public String getQuery()
            {
                return query;
            }

            /**
             * @param query the query to set
             */
            public void setQuery(String query)
            {
                this.query = query;
            }

            /**
             * @return the score
             */
            public float getScore()
            {
                return score;
            }

            /**
             * @param score the score to set
             */
            public void setScore(float score)
            {
                this.score = score;
            }

            /**
             * @return the metadata
             */
            public Metadata getMetadata()
            {
                return metadata;
            }

            /**
             * @param metadata the metadata to set
             */
            public void setMetadata(Metadata metadata)
            {
                this.metadata = metadata;
            }

            public class Metadata
            {
                @SerializedName("document_id")
                private String documentID;

                /**
                 * @return the documentID
                 */
                public String getDocumentID()
                {
                    return documentID;
                }

                /**
                 * @param documentID the documentID to set
                 */
                public void setDocumentID(String documentID)
                {
                    this.documentID = documentID;
                }
            }
        }
    }
}
