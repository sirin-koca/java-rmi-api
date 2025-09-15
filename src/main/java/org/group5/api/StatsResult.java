package org.group5.api;

import java.io.Serializable;

public class StatsResult implements Serializable {
    private final long value;         // numeric result
    private final long execTimeMs;    // measured at server

    public StatsResult(long value, long execTimeMs) {
        this.value = value;
        this.execTimeMs = execTimeMs;
    }
    public long getValue() { return value; }
    public long getExecTimeMs() { return execTimeMs; }
}
