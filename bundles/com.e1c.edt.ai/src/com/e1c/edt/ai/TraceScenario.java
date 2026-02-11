/**
 *
 */
package com.e1c.edt.ai;

import java.util.concurrent.atomic.AtomicInteger;

public class TraceScenario
    implements ITraceScenario
{
    private AtomicInteger isSessionExpired = new AtomicInteger(0);

    @Override
    public void EpireSession(int counter)
    {
        isSessionExpired.set(counter);
    }

    @Override
    public boolean isSessionExpired()
    {
        return isSessionExpired.getAndDecrement() > 0;
    }
}
