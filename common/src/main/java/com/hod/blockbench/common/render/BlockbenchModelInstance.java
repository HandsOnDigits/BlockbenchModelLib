package com.hod.blockbench.common.render;

import com.hod.blockbench.animation.AnimationController;
import com.hod.blockbench.animation.AnimationState;
import com.hod.blockbench.animation.AnimationTransform;
import com.hod.blockbench.common.geometry.ModelGeometry;
import com.hod.blockbench.common.geometry.ModelGeometryBuilder;
import com.hod.blockbench.common.geometry.ModelGeometryHierarchy;
import com.hod.blockbench.common.render.texture.MinecraftModelTextureResolver;
import com.hod.blockbench.common.render.texture.ModelTextureManager;
import com.hod.blockbench.common.render.texture.ModelTextureRegistration;
import com.hod.blockbench.model.BlockbenchModel;
import com.hod.blockbench.model.ModelAnimation;
import com.mojang.blaze3d.vertex.PoseStack;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Runtime instance of a Blockbench model.
 *
 * Owns generated geometry, registered model textures, and animation playback
 * state. Minecraft rendering resources such as PoseStack and MultiBufferSource
 * remain owned by the caller.
 *
 * Must be created, rendered, updated, and closed on Minecraft's render thread.
 */
public final class BlockbenchModelInstance implements AutoCloseable {

    private final BlockbenchModel model;
    private final ModelGeometry geometry;
    private final ModelGeometryHierarchy hierarchy;
    private final ModelTextureRegistration textures;
    private final boolean doubleSided;

    private final AnimationController animationController =
        new AnimationController();

    private boolean closed;

    private BlockbenchModelInstance(
        BlockbenchModel model,
        ModelGeometry geometry,
        ModelGeometryHierarchy hierarchy,
        ModelTextureRegistration textures,
        boolean doubleSided
    ) {
        this.model = Objects.requireNonNull(model);
        this.geometry = Objects.requireNonNull(geometry);
        this.hierarchy = Objects.requireNonNull(hierarchy);
        this.textures = Objects.requireNonNull(textures);
        this.doubleSided = doubleSided;
    }

    public static BlockbenchModelInstance create(
        BlockbenchModel model,
        ModelTextureManager textureManager,
        ResourceManager resourceManager,
        boolean doubleSided
    ) throws IOException {
        Objects.requireNonNull(model, "Model cannot be null.");
        Objects.requireNonNull(
            textureManager,
            "Texture manager cannot be null."
        );
        Objects.requireNonNull(
            resourceManager,
            "ResourceManager cannot be null."
        );

        ModelGeometry geometry = ModelGeometryBuilder.build(model);
        ModelGeometryHierarchy hierarchy = ModelGeometryBuilder.buildHierarchy(
            model
        );

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
                hierarchy,
                textures,
                doubleSided
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
     * Creates a runtime instance from an already loaded Blockbench model.
     */
    public static BlockbenchModelInstance create(
        BlockbenchModel model,
        ModelTextureManager textureManager,
        ResourceManager resourceManager
    ) throws IOException {
        return create(model, textureManager, resourceManager, false);
    }

    /**
     * Renders this model instance.
     *
     * Static rendering uses the original flat geometry path.
     * Playing, paused, and finished animations use hierarchical geometry.
     */
    public void render(
        PoseStack poseStack,
        MultiBufferSource buffers,
        int packedLight,
        int packedOverlay
    ) {
        ensureOpen();

        AnimationState state = animationController.state();

        Map<String, AnimationTransform> animations =
            state.animation() != null &&
            state.status() !=
                com.hod.blockbench.animation.AnimationPlaybackStatus.STOPPED
                ? state.evaluate()
                : Map.of();

        BlockbenchModelRenderer.render(
            hierarchy,
            poseStack,
            textures,
            buffers,
            doubleSided,
            animations,
            packedLight,
            packedOverlay
        );
    }

    // ---------------------------------------------------------------------
    // Animation playback
    // ---------------------------------------------------------------------

    /**
     * Starts playing the supplied animation from its beginning.
     */
    public void playAnimation(ModelAnimation animation) {
        ensureOpen();
        animationController.play(
            Objects.requireNonNull(animation, "Animation cannot be null.")
        );
    }

    /**
     * Finds an animation by UUID and starts playing it.
     *
     * @throws IllegalArgumentException if no animation with that UUID exists
     */
    public void playAnimation(String animationUuid) {
        ensureOpen();

        String uuid = Objects.requireNonNull(
            animationUuid,
            "Animation UUID cannot be null."
        );

        ModelAnimation animation = model
            .findAnimation(uuid)
            .orElseThrow(() ->
                new IllegalArgumentException("Unknown animation UUID: " + uuid)
            );

        playAnimation(animation);
    }

    /**
     * Finds an animation by name and starts playing it.
     *
     * @throws IllegalArgumentException if no animation exists or the name is ambiguous
     */
    public void playAnimationByName(String animationName) {
        ensureOpen();

        var matches = model.findAnimationsByName(
            Objects.requireNonNull(
                animationName,
                "Animation name cannot be null."
            )
        );

        if (matches.isEmpty()) {
            throw new IllegalArgumentException(
                "Unknown animation name: " + animationName
            );
        }

        if (matches.size() > 1) {
            throw new IllegalArgumentException(
                "Multiple animations have the name: " +
                    animationName +
                    ". Use an animation UUID instead."
            );
        }

        playAnimation(matches.get(0));
    }

    /**
     * Pauses the current animation.
     */
    public void pauseAnimation() {
        ensureOpen();
        animationController.pause();
    }

    /**
     * Resumes the current animation.
     */
    public void resumeAnimation() {
        ensureOpen();
        animationController.resume();
    }

    /**
     * Stops the current animation and resets its playback time.
     */
    public void stopAnimation() {
        ensureOpen();
        animationController.stop();
    }

    /**
     * Advances animation playback.
     *
     * Call once per game tick or render frame, passing elapsed seconds.
     */
    public void updateAnimation(float deltaSeconds) {
        ensureOpen();
        animationController.update(deltaSeconds);
    }

    /**
     * Changes the playback speed.
     */
    public void setAnimationSpeed(float speed) {
        ensureOpen();
        animationController.setSpeed(speed);
    }

    /**
     * Returns the current animation playback state.
     */
    public AnimationState animationState() {
        ensureOpen();
        return animationController.state();
    }

    // ---------------------------------------------------------------------
    // Model information
    // ---------------------------------------------------------------------

    public boolean isDoubleSided() {
        return doubleSided;
    }

    public BlockbenchModel model() {
        return model;
    }

    /**
     * Returns the original flat geometry representation.
     */
    public ModelGeometry geometry() {
        return geometry;
    }

    /**
     * Returns the hierarchical geometry representation.
     */
    public ModelGeometryHierarchy hierarchy() {
        return hierarchy;
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
