package com.hod.blockbench.common.geometry;

import com.hod.blockbench.model.ModelVector3;

import java.util.Objects;

/**
 * Immutable 4x4 transformation matrix.
 *
 * Uses row-major storage and column-vector transformations.
 * Transform composition follows:
 *
 * parent.multiply(child)
 */
public final class ModelTransform {

    private static final int MATRIX_SIZE = 16;

    private final float[] matrix;

    private ModelTransform(float[] matrix) {
        if (matrix.length != MATRIX_SIZE) {
            throw new IllegalArgumentException(
                    "A transformation matrix must contain 16 values."
            );
        }

        this.matrix = matrix.clone();
    }

    public static ModelTransform identity() {
        return new ModelTransform(new float[]{
                1, 0, 0, 0,
                0, 1, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        });
    }

    public static ModelTransform translation(
            float x,
            float y,
            float z
    ) {
        return new ModelTransform(new float[]{
                1, 0, 0, x,
                0, 1, 0, y,
                0, 0, 1, z,
                0, 0, 0, 1
        });
    }

    public static ModelTransform rotationX(float degrees) {
        float radians = (float) Math.toRadians(degrees);

        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        return new ModelTransform(new float[]{
                1, 0, 0, 0,
                0, cos, -sin, 0,
                0, sin, cos, 0,
                0, 0, 0, 1
        });
    }

    public static ModelTransform rotationY(float degrees) {
        float radians = (float) Math.toRadians(degrees);

        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        return new ModelTransform(new float[]{
                cos, 0, sin, 0,
                0, 1, 0, 0,
                -sin, 0, cos, 0,
                0, 0, 0, 1
        });
    }

    public static ModelTransform rotationZ(float degrees) {
        float radians = (float) Math.toRadians(degrees);

        float cos = (float) Math.cos(radians);
        float sin = (float) Math.sin(radians);

        return new ModelTransform(new float[]{
                cos, -sin, 0, 0,
                sin, cos, 0, 0,
                0, 0, 1, 0,
                0, 0, 0, 1
        });
    }

    /**
     * Creates an Euler rotation using Blockbench's X, Y and Z
     * rotation components, in degrees.
     *
     * The resulting matrix applies X first, then Y, then Z.
     */
    public static ModelTransform rotation(
            ModelVector3 rotation
    ) {
        Objects.requireNonNull(rotation, "Rotation cannot be null.");

        return rotationZ(rotation.z())
                .multiply(rotationY(rotation.y()))
                .multiply(rotationX(rotation.x()));
    }

    /**
     * Creates a rotation around a specific origin.
     *
     * The origin remains stationary during rotation.
     */
    public static ModelTransform aroundOrigin(
            ModelVector3 origin,
            ModelVector3 rotation
    ) {
        Objects.requireNonNull(origin, "Origin cannot be null.");

        ModelTransform toOrigin = translation(
                origin.x(),
                origin.y(),
                origin.z()
        );

        ModelTransform fromOrigin = translation(
                -origin.x(),
                -origin.y(),
                -origin.z()
        );

        return toOrigin
                .multiply(rotation(rotation))
                .multiply(fromOrigin);
    }

    /**
     * Multiplies this matrix by another matrix.
     *
     * The resulting transformation applies the other
     * transformation first, followed by this transformation.
     */
    public ModelTransform multiply(ModelTransform other) {
        Objects.requireNonNull(other, "Other transform cannot be null.");

        float[] result = new float[MATRIX_SIZE];

        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                float value = 0;

                for (int i = 0; i < 4; i++) {
                    value += matrix[row * 4 + i]
                            * other.matrix[i * 4 + column];
                }

                result[row * 4 + column] = value;
            }
        }

        return new ModelTransform(result);
    }

    /**
     * Transforms a position, including translation.
     */
    public ModelVector3 transformPosition(ModelVector3 position) {
        Objects.requireNonNull(position, "Position cannot be null.");

        float x = matrix[0] * position.x()
                + matrix[1] * position.y()
                + matrix[2] * position.z()
                + matrix[3];

        float y = matrix[4] * position.x()
                + matrix[5] * position.y()
                + matrix[6] * position.z()
                + matrix[7];

        float z = matrix[8] * position.x()
                + matrix[9] * position.y()
                + matrix[10] * position.z()
                + matrix[11];

        return new ModelVector3(x, y, z);
    }

    /**
     * Transforms a direction without applying translation.
     */
    public ModelVector3 transformDirection(ModelVector3 direction) {
        Objects.requireNonNull(direction, "Direction cannot be null.");

        float x = matrix[0] * direction.x()
                + matrix[1] * direction.y()
                + matrix[2] * direction.z();

        float y = matrix[4] * direction.x()
                + matrix[5] * direction.y()
                + matrix[6] * direction.z();

        float z = matrix[8] * direction.x()
                + matrix[9] * direction.y()
                + matrix[10] * direction.z();

        return new ModelVector3(x, y, z);
    }
}