/*
 * Copyright (C) 2024, 1C
 */
package org.e1c.edt.ai.assistent.model;

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
}
