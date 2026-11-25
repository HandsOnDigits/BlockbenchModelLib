package com.hod.blockbench.common.render.texture;

import java.util.Objects;

public record ResolvedModelTexture(
        int textureIndex,
        String name,
        byte[] imageData
) {

    public ResolvedModelTexture {
        if (textureIndex < 0) {
            throw new IllegalArgumentException(
                    "Texture index cannot be negative."
            );
        }

        Objects.requireNonNull(name, "Texture name cannot be null.");
        Objects.requireNonNull(imageData, "Image data cannot be null.");

        if (imageData.length == 0) {
            throw new IllegalArgumentException(
                    "Image data cannot be empty."
            );
        }

        imageData = imageData.clone();
    }

    @Override
    public byte[] imageData() {
        return imageData.clone();
    }
}