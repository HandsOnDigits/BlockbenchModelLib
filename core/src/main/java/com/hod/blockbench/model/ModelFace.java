package com.hod.blockbench.model;

public record ModelFace(
        Direction direction,
        ModelUv uv,
        int texture,
        int rotation
) {
    public enum Direction {
        NORTH,
        EAST,
        SOUTH,
        WEST,
        UP,
        DOWN
    }

    public ModelUv normalizedUv(ModelTexture texture) {
        if (texture == null) {
            throw new IllegalArgumentException(
                    "Texture cannot be null."
            );
        }

        if (texture.uvWidth() <= 0 || texture.uvHeight() <= 0) {
            throw new IllegalArgumentException(
                    "Texture UV dimensions must be greater than zero."
            );
        }

        return new ModelUv(
                uv.u1() / texture.uvWidth(),
                uv.v1() / texture.uvHeight(),
                uv.u2() / texture.uvWidth(),
                uv.v2() / texture.uvHeight()
        );
    }

    public record ModelUv(
            float u1,
            float v1,
            float u2,
            float v2
    ) {
    }
}