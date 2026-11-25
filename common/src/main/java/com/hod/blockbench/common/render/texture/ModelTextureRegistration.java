package com.hod.blockbench.common.render.texture;

import net.minecraft.client.renderer.texture.TextureManager;
import net.minecraft.resources.ResourceLocation;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public final class ModelTextureRegistration implements AutoCloseable {

    private final TextureManager textureManager;
    private final Map<Integer, ResourceLocation> textures;

    private boolean closed;

    ModelTextureRegistration(
            TextureManager textureManager,
            Map<Integer, ResourceLocation> textures
    ) {
        this.textureManager = Objects.requireNonNull(
                textureManager,
                "TextureManager cannot be null."
        );

        this.textures = Map.copyOf(
                Objects.requireNonNull(
                        textures,
                        "Textures cannot be null."
                )
        );
    }

    /**
     * Returns the registered texture for an original model texture index.
     */
    public Optional<ResourceLocation> get(int textureIndex) {
        ensureOpen();
        return Optional.ofNullable(textures.get(textureIndex));
    }

    /**
     * Returns the registered texture or throws if it is unavailable.
     */
    public ResourceLocation require(int textureIndex) {
        return get(textureIndex).orElseThrow(() ->
                new IllegalArgumentException(
                        "No registered texture for model texture index "
                                + textureIndex
                )
        );
    }

    /**
     * Returns an immutable map of registered textures.
     */
    public Map<Integer, ResourceLocation> textures() {
        ensureOpen();
        return textures;
    }

    public boolean isClosed() {
        return closed;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(
                    "Texture registration has already been closed."
            );
        }
    }

    /**
     * Releases all registered textures.
     *
     * Must be called on Minecraft's render thread.
     * Repeated calls have no effect.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        RuntimeException failure = null;

        for (ResourceLocation location : textures.values()) {
            try {
                textureManager.release(location);
            } catch (RuntimeException exception) {
                if (failure == null) {
                    failure = new IllegalStateException(
                            "One or more model textures could not be released."
                    );
                }

                failure.addSuppressed(exception);
            }
        }

        if (failure != null) {
            throw failure;
        }
    }
}