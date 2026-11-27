package com.hod.blockbench.model;

/**
 * Stores one Blockbench animation data point.
 *
 * Values are retained as strings because Blockbench can include
 * whitespace and newline characters in coordinate values.
 */
public record AnimationDataPoint(String x, String y, String z) {
    public AnimationDataPoint {
        x = x != null ? x : "0";
        y = y != null ? y : "0";
        z = z != null ? z : "0";
    }

    public float xValue() {
        return Float.parseFloat(x.trim());
    }

    public float yValue() {
        return Float.parseFloat(y.trim());
    }

    public float zValue() {
        return Float.parseFloat(z.trim());
    }

    public ModelVector3 toVector3() {
        return new ModelVector3(xValue(), yValue(), zValue());
    }
}
