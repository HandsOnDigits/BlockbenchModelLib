package com.hod.blockbench.model;

import java.util.List;

public record AnimationKeyframe(
    String uuid,
    String channel,
    float time,
    String interpolation,
    int color,
    List<AnimationDataPoint> dataPoints
) {
    public AnimationKeyframe {
        dataPoints = List.copyOf(dataPoints);
    }

    /**
     * Returns the first data point, or null if none exists.
     */
    public AnimationDataPoint firstDataPoint() {
        return dataPoints.isEmpty() ? null : dataPoints.get(0);
    }
}
