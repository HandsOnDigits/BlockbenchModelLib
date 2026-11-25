package com.hod.blockbench.common.render.texture;

import com.mojang.blaze3d.platform.NativeImage;

import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class ModelTextureManager {

    private final TextureManager textureManager;

    public ModelTextureManager(TextureManager textureManager) {
        this.textureManager = Objects.requireNonNull(
                textureManager,
                "TextureManager cannot be null."
        );
    }

    /**
     * Registers resolved textures with Minecraft.
     *
     * Original texture indices are preserved, so geometry texture
     * references remain valid even when some textures are absent.
     *
     * Must be called on Minecraft's render thread.
     */
    public ModelTextureRegistration register(
            List<ResolvedModelTexture> resolvedTextures
    ) throws IOException {
        Objects.requireNonNull(
                resolvedTextures,
                "Resolved textures cannot be null."
        );

        Map<Integer, ResourceLocation> registered = new HashMap<>();

        try {
            for (ResolvedModelTexture texture : resolvedTextures) {
                Objects.requireNonNull(
                        texture,
                        "Resolved texture cannot be null."
                );

                int index = texture.textureIndex();

                if (registered.containsKey(index)) {
                    throw new IllegalArgumentException(
                            "Duplicate resolved texture index: " + index
                    );
                }

                NativeImage image = NativeImage.read(texture.imageData());
                DynamicTexture dynamicTexture = null;

                try {
                    dynamicTexture = new DynamicTexture(image);

                    // DynamicTexture now owns the NativeImage.
                    image = null;

                    ResourceLocation location = textureManager.register(
                            "blockbench",
                            dynamicTexture
                    );

                    // TextureManager now owns the DynamicTexture.
                    dynamicTexture = null;

                    registered.put(index, location);
                } finally {
                    if (dynamicTexture != null) {
                        dynamicTexture.close();
                    }

                    if (image != null) {
                        image.close();
                    }
                }
            }

            return new ModelTextureRegistration(
                    textureManager,
                    registered
            );
        } catch (IOException | RuntimeException exception) {
            releaseAll(registered);
            throw exception;
        }
    }

    private void releaseAll(
            Map<Integer, ResourceLocation> registered
    ) {
        for (ResourceLocation location : registered.values()) {
            textureManager.release(location);
        }
    }
}