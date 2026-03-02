/**
 *
 */
package com.e1c.edt.ai;

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;

public class TraceScenario
    implements ITraceScenario
{
    private final EnumMap<TraceScenarioType, AtomicBoolean> scenarios = new EnumMap<>(TraceScenarioType.class);

    public TraceScenario()
    {
        for (TraceScenarioType type : TraceScenarioType.values())
        {
            scenarios.put(type, new AtomicBoolean(false));
        }
    }

    @Override
    public void activate(TraceScenarioType type)
    {
        for (TraceScenarioType scenarioType : TraceScenarioType.values())
        {
            scenarios.get(scenarioType).set(scenarioType == type);
        }
    }

    @Override
    public TraceScenarioType getActive()
    {
        for (TraceScenarioType type : TraceScenarioType.values())
        {
            if (scenarios.get(type).get())
            {
                return type;
            }
        }

        return TraceScenarioType.NONE;
    }

    @Override
    public void deactivate()
    {
        for (TraceScenarioType type : TraceScenarioType.values())
        {
            scenarios.get(type).set(false);
        }
    }
}
