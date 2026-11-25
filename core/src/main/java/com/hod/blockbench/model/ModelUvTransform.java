package com.hod.blockbench.model;

import java.util.List;

public final class ModelUvTransform {

    private ModelUvTransform() {
    }

    /**
     * Returns the four normalized UV corners of a face,
     * accounting for Blockbench's UV rotation.
     *
     * Corner order:
     * 0 = top-left
     * 1 = top-right
     * 2 = bottom-right
     * 3 = bottom-left
     *
     * Rotation follows Blockbench's UVToLocal convention.
     */
    public static List<UvPoint> getCorners(
            ModelFace face,
            ModelTexture texture
    ) {
        if (face == null) {
            throw new IllegalArgumentException("Face cannot be null.");
        }

        if (texture == null) {
            throw new IllegalArgumentException("Texture cannot be null.");
        }

        int rotation = face.rotation();

        if (rotation != 0 && rotation != 90
                && rotation != 180 && rotation != 270) {
            throw new IllegalArgumentException(
                    "Unsupported UV rotation: " + rotation
            );
        }

        ModelFace.ModelUv uv = face.normalizedUv(texture);

        List<UvPoint> corners = List.of(
                new UvPoint(uv.u1(), uv.v1()),
                new UvPoint(uv.u2(), uv.v1()),
                new UvPoint(uv.u2(), uv.v2()),
                new UvPoint(uv.u1(), uv.v2())
        );

        int rotationSteps = rotation / 90;

        return List.of(
                corners.get(rotationSteps % 4),
                corners.get((rotationSteps + 1) % 4),
                corners.get((rotationSteps + 2) % 4),
                corners.get((rotationSteps + 3) % 4)
        );
    }

    public record UvPoint(
            float u,
            float v
    ) {
    }
}