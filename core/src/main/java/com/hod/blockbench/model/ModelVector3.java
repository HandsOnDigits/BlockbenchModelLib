package com.hod.blockbench.model;

public record ModelVector3(
        float x,
        float y,
        float z
) {
    public static final ModelVector3 ZERO = new ModelVector3(0, 0, 0);

    public ModelVector3 add(ModelVector3 other) {
        return new ModelVector3(
                x + other.x,
                y + other.y,
                z + other.z
        );
    }

    public ModelVector3 subtract(ModelVector3 other) {
        return new ModelVector3(
                x - other.x,
                y - other.y,
                z - other.z
        );
    }

    public ModelVector3 multiply(float scalar) {
        return new ModelVector3(
                x * scalar,
                y * scalar,
                z * scalar
        );
    }
}