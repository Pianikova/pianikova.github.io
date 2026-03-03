/**
 *
 */
package com.e1c.edt.ai;

public interface ITraceScenario
{
    public void activate(TraceScenarioType type);

    public TraceScenarioType getActive();

    public void deactivate();
}
