package com.hod.blockbench.common.render;

import com.hod.blockbench.common.geometry.ModelGeometry;
import com.hod.blockbench.common.geometry.ModelQuad;
import com.hod.blockbench.common.geometry.ModelVertex;
import com.hod.blockbench.common.render.texture.ModelTextureRegistration;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.IntFunction;

public final class BlockbenchModelRenderer {

    /**
     * Blockbench model coordinates use 16 units per Minecraft block.
     */
    public static final float BLOCKBENCH_UNITS_PER_BLOCK = 16.0f;

    private BlockbenchModelRenderer() {
    }

    /**
     * Renders geometry using automatically selected Minecraft render buffers.
     *
     * Uses entityCutoutNoCull for all textures.
     */
    public static void render(
            ModelGeometry geometry,
            PoseStack poseStack,
            ModelTextureRegistration textures,
            MultiBufferSource buffers,
            int packedLight,
            int packedOverlay
    ) {
        render(
                geometry,
                poseStack,
                textures,
                buffers,
                (index, location) ->
                        RenderType.entityCutoutNoCull(location),
                packedLight,
                packedOverlay
        );
    }

    /**
     * Renders geometry with a configurable RenderType for each texture.
     *
     * The RenderType factory receives the original texture index and
     * its registered ResourceLocation.
     */
    public static void render(
            ModelGeometry geometry,
            PoseStack poseStack,
            ModelTextureRegistration textures,
            MultiBufferSource buffers,
            BiFunction<Integer, ResourceLocation, RenderType> renderTypes,
            int packedLight,
            int packedOverlay
    ) {
        Objects.requireNonNull(geometry, "Geometry cannot be null.");
        Objects.requireNonNull(poseStack, "PoseStack cannot be null.");
        Objects.requireNonNull(textures, "Textures cannot be null.");
        Objects.requireNonNull(buffers, "Buffer source cannot be null.");
        Objects.requireNonNull(renderTypes, "Render type factory cannot be null.");

        Map<Integer, VertexConsumer> consumers = new HashMap<>();

        for (ModelQuad quad : geometry.quads()) {
            int textureIndex = quad.texture();

            VertexConsumer consumer = consumers.computeIfAbsent(
                    textureIndex,
                    index -> {
                        ResourceLocation location = textures.require(index);

                        RenderType renderType = Objects.requireNonNull(
                                renderTypes.apply(index, location),
                                "RenderType factory returned null for texture "
                                        + index
                        );

                        return buffers.getBuffer(renderType);
                    }
            );

            renderQuad(
                    quad,
                    poseStack.last(),
                    consumer,
                    packedLight,
                    packedOverlay
            );
        }
    }

    /**
     * Renders geometry using a VertexConsumer selected by texture index.
     *
     * The supplied PoseStack is not modified.
     */
    public static void render(
            ModelGeometry geometry,
            PoseStack poseStack,
            IntFunction<VertexConsumer> consumers,
            int packedLight,
            int packedOverlay
    ) {
        Objects.requireNonNull(geometry, "Geometry cannot be null.");
        Objects.requireNonNull(poseStack, "PoseStack cannot be null.");
        Objects.requireNonNull(consumers, "Consumer provider cannot be null.");

        PoseStack.Pose pose = poseStack.last();

        for (ModelQuad quad : geometry.quads()) {
            VertexConsumer consumer = Objects.requireNonNull(
                    consumers.apply(quad.texture()),
                    "No VertexConsumer supplied for texture index "
                            + quad.texture()
            );

            renderQuad(
                    quad,
                    pose,
                    consumer,
                    packedLight,
                    packedOverlay
            );
        }
    }

    /**
     * Renders all geometry into a single VertexConsumer.
     *
     * Only use when all faces share the same render type and texture.
     */
    public static void render(
            ModelGeometry geometry,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay
    ) {
        Objects.requireNonNull(consumer, "VertexConsumer cannot be null.");

        render(
                geometry,
                poseStack,
                ignored -> consumer,
                packedLight,
                packedOverlay
        );
    }

    private static void renderQuad(
            ModelQuad quad,
            PoseStack.Pose pose,
            VertexConsumer consumer,
            int packedLight,
            int packedOverlay
    ) {
        for (ModelVertex vertex : quad.vertices()) {
            var position = vertex.position();
            var normal = vertex.normal();

            consumer.addVertex(
                            pose,
                            position.x() / BLOCKBENCH_UNITS_PER_BLOCK,
                            position.y() / BLOCKBENCH_UNITS_PER_BLOCK,
                            position.z() / BLOCKBENCH_UNITS_PER_BLOCK
                    )
                    .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                    .setUv(vertex.u(), vertex.v())
                    .setOverlay(packedOverlay)
                    .setLight(packedLight)
                    .setNormal(
                            pose,
                            normal.x(),
                            normal.y(),
                            normal.z()
                    );
        }
    }
}