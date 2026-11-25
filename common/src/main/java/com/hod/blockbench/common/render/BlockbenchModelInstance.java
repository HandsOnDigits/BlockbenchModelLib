package com.hod.blockbench.common.render;

import com.hod.blockbench.common.geometry.ModelGeometry;
import com.hod.blockbench.common.geometry.ModelGeometryBuilder;
import com.hod.blockbench.common.render.texture.MinecraftModelTextureResolver;
import com.hod.blockbench.common.render.texture.ModelTextureManager;
import com.hod.blockbench.common.render.texture.ModelTextureRegistration;
import com.hod.blockbench.model.BlockbenchModel;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Runtime instance of a Blockbench model.
 *
 * Owns the generated geometry and registered model textures.
 * Minecraft rendering resources such as PoseStack and MultiBufferSource
 * remain owned by the caller.
 *
 * Must be created, rendered, and closed on Minecraft's render thread.
 */
public final class BlockbenchModelInstance implements AutoCloseable {

    private final BlockbenchModel model;
    private final ModelGeometry geometry;
    private final ModelTextureRegistration textures;

    private boolean closed;

    private BlockbenchModelInstance(
            BlockbenchModel model,
            ModelGeometry geometry,
            ModelTextureRegistration textures
    ) {
        this.model = Objects.requireNonNull(model);
        this.geometry = Objects.requireNonNull(geometry);
        this.textures = Objects.requireNonNull(textures);
    }

    /**
     * Creates a runtime instance from an already loaded Blockbench model.
     *
     * @param model loaded Blockbench model
     * @param textureManager manager used to register textures
     * @param resourceManager Minecraft resource manager used to resolve external textures
     * @return initialized model instance
     * @throws IOException if a texture cannot be loaded
     */
    public static BlockbenchModelInstance create(
            BlockbenchModel model,
            ModelTextureManager textureManager,
            ResourceManager resourceManager
    ) throws IOException {
        Objects.requireNonNull(model, "Model cannot be null.");
        Objects.requireNonNull(textureManager, "Texture manager cannot be null.");
        Objects.requireNonNull(resourceManager, "ResourceManager cannot be null.");

        ModelGeometry geometry = ModelGeometryBuilder.build(model);

        var resolvedTextures = MinecraftModelTextureResolver.resolve(
                model.textures(),
                resourceManager
        );

        ModelTextureRegistration textures = textureManager.register(
                resolvedTextures
        );

        try {
            return new BlockbenchModelInstance(
                    model,
                    geometry,
                    textures
            );
        } catch (RuntimeException | Error exception) {
            try {
                textures.close();
            } catch (RuntimeException cleanupException) {
                exception.addSuppressed(cleanupException);
            }

            throw exception;
        }
    }

    /**
     * Renders this model instance.
     */
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        ensureOpen();

        BlockbenchModelRenderer.render(
                geometry,
                poseStack,
                textures,
                buffers,
                packedLight,
                packedOverlay
        );
    }

    public BlockbenchModel model() {
        return model;
    }

    public ModelGeometry geometry() {
        return geometry;
    }

    /**
     * Returns a registered texture by its original Blockbench texture index.
     */
    public Optional<ResourceLocation> texture(int textureIndex) {
        ensureOpen();
        return textures.get(textureIndex);
    }

    public boolean isClosed() {
        return closed;
    }

    private void ensureOpen() {
        if (closed) {
            throw new IllegalStateException(
                    "Cannot use a closed BlockbenchModelInstance."
            );
        }
    }

    /**
     * Releases registered Minecraft textures.
     *
     * Must be called on Minecraft's render thread.
     * Safe to call multiple times.
     */
    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;
        textures.close();
    }
}