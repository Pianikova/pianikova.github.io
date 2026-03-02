/**
 *
 */
package com.e1c.edt.ai;

import java.util.EnumMap;
import java.util.concurrent.atomic.AtomicBoolean;

import com.google.inject.Inject;

public class TraceScenario
    implements ITraceScenario
{
    private final EnumMap<TraceScenarioType, AtomicBoolean> scenarios = new EnumMap<>(TraceScenarioType.class);
    private final IStateService stateService;

    @Inject
    public TraceScenario(IStateService stateService)
    {
        this.stateService = stateService;
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
        if (type == TraceScenarioType.SSL_ERROR)
        {
            stateService.setState(ServiceState.SETTINGS_CHANGED);
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
