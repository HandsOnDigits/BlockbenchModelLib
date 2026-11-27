package com.hod.blockbench.common.render;

import com.hod.blockbench.animation.AnimationTransform;
import com.hod.blockbench.common.geometry.ModelGeometry;
import com.hod.blockbench.common.geometry.ModelGeometryHierarchy;
import com.hod.blockbench.common.geometry.ModelGeometryNode;
import com.hod.blockbench.common.geometry.ModelQuad;
import com.hod.blockbench.common.geometry.ModelVertex;
import com.hod.blockbench.common.render.texture.ModelTextureRegistration;
import com.hod.blockbench.model.ModelVector3;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.IntFunction;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

public final class BlockbenchModelRenderer {

    /**
     * Blockbench model coordinates use 16 units per Minecraft block.
     */
    public static final float BLOCKBENCH_UNITS_PER_BLOCK = 16.0f;

    private BlockbenchModelRenderer() {}

    /**
     * Renders geometry using automatically selected Minecraft render buffers.
     *
     * Backface culling is enabled by default.
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
            false,
            packedLight,
            packedOverlay
        );
    }

    /**
     * Renders geometry with configurable double-sided rendering.
     *
     * @param doubleSided true to disable backface culling
     */
    public static void render(
        ModelGeometry geometry,
        PoseStack poseStack,
        ModelTextureRegistration textures,
        MultiBufferSource buffers,
        boolean doubleSided,
        int packedLight,
        int packedOverlay
    ) {
        render(
            geometry,
            poseStack,
            textures,
            buffers,
            (index, location) ->
                doubleSided
                    ? RenderType.entityCutoutNoCull(location)
                    : RenderType.entityCutout(location),
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
        Objects.requireNonNull(
            renderTypes,
            "Render type factory cannot be null."
        );

        Map<Integer, VertexConsumer> consumers = new HashMap<>();

        for (ModelQuad quad : geometry.quads()) {
            int textureIndex = quad.texture();

            VertexConsumer consumer = consumers.computeIfAbsent(
                textureIndex,
                index -> {
                    ResourceLocation location = textures.require(index);

                    RenderType renderType = Objects.requireNonNull(
                        renderTypes.apply(index, location),
                        "RenderType factory returned null for texture " + index
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
                "No VertexConsumer supplied for texture index " + quad.texture()
            );

            renderQuad(quad, pose, consumer, packedLight, packedOverlay);
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

            consumer
                .addVertex(
                    pose,
                    position.x() / BLOCKBENCH_UNITS_PER_BLOCK,
                    position.y() / BLOCKBENCH_UNITS_PER_BLOCK,
                    position.z() / BLOCKBENCH_UNITS_PER_BLOCK
                )
                .setColor(1.0f, 1.0f, 1.0f, 1.0f)
                .setUv(vertex.u(), vertex.v())
                .setOverlay(packedOverlay)
                .setLight(packedLight)
                .setNormal(pose, normal.x(), normal.y(), normal.z());
        }
    }

    /**
     * Renders hierarchical geometry with configurable double-sided rendering.
     */
    public static void render(
        ModelGeometryHierarchy geometry,
        PoseStack poseStack,
        ModelTextureRegistration textures,
        MultiBufferSource buffers,
        boolean doubleSided,
        int packedLight,
        int packedOverlay
    ) {
        render(
            geometry,
            poseStack,
            textures,
            buffers,
            (index, location) ->
                doubleSided
                    ? RenderType.entityCutoutNoCull(location)
                    : RenderType.entityCutout(location),
            packedLight,
            packedOverlay
        );
    }

    /**
     * Renders hierarchical geometry with a configurable RenderType per texture.
     */
    public static void render(
        ModelGeometryHierarchy geometry,
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
        Objects.requireNonNull(
            renderTypes,
            "Render type factory cannot be null."
        );

        Map<Integer, VertexConsumer> consumers = new HashMap<>();

        IntFunction<VertexConsumer> consumerProvider = index ->
            consumers.computeIfAbsent(index, textureIndex -> {
                ResourceLocation location = textures.require(textureIndex);

                RenderType renderType = Objects.requireNonNull(
                    renderTypes.apply(textureIndex, location),
                    "RenderType factory returned null for texture " +
                        textureIndex
                );

                return buffers.getBuffer(renderType);
            });

        renderHierarchy(
            geometry,
            poseStack,
            consumerProvider,
            Map.of(),
            packedLight,
            packedOverlay
        );
    }

    /**
     * Renders hierarchical geometry using a VertexConsumer selected by texture.
     */
    public static void render(
        ModelGeometryHierarchy geometry,
        PoseStack poseStack,
        IntFunction<VertexConsumer> consumers,
        int packedLight,
        int packedOverlay
    ) {
        Objects.requireNonNull(geometry, "Geometry cannot be null.");
        Objects.requireNonNull(poseStack, "PoseStack cannot be null.");
        Objects.requireNonNull(consumers, "Consumer provider cannot be null.");

        renderHierarchy(
            geometry,
            poseStack,
            consumers,
            Map.of(),
            packedLight,
            packedOverlay
        );
    }

    /**
     * Renders hierarchical geometry into a single VertexConsumer.
     */
    public static void render(
        ModelGeometryHierarchy geometry,
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

    private static void renderHierarchy(
        ModelGeometryHierarchy geometry,
        PoseStack poseStack,
        IntFunction<VertexConsumer> consumers,
        Map<String, AnimationTransform> animations,
        int packedLight,
        int packedOverlay
    ) {
        renderQuads(
            geometry.rootQuads(),
            poseStack,
            consumers,
            packedLight,
            packedOverlay
        );

        for (ModelGeometryNode root : geometry.roots()) {
            renderNode(
                root,
                poseStack,
                consumers,
                animations,
                packedLight,
                packedOverlay
            );
        }
    }

    private static void renderNode(
        ModelGeometryNode node,
        PoseStack poseStack,
        IntFunction<VertexConsumer> consumers,
        Map<String, AnimationTransform> animations,
        int packedLight,
        int packedOverlay
    ) {
        poseStack.pushPose();

        try {
            applyNodeTransform(node, poseStack, animations.get(node.uuid()));

            renderQuads(
                node.quads(),
                poseStack,
                consumers,
                packedLight,
                packedOverlay
            );

            for (ModelGeometryNode child : node.children()) {
                renderNode(
                    child,
                    poseStack,
                    consumers,
                    animations,
                    packedLight,
                    packedOverlay
                );
            }
        } finally {
            poseStack.popPose();
        }
    }

    private static void applyNodeTransform(
        ModelGeometryNode node,
        PoseStack poseStack,
        AnimationTransform animation
    ) {
        ModelVector3 origin =
            node.origin() != null ? node.origin() : ModelVector3.ZERO;

        ModelVector3 baseRotation =
            node.rotation() != null ? node.rotation() : ModelVector3.ZERO;

        ModelVector3 position =
            animation != null ? animation.position() : ModelVector3.ZERO;

        ModelVector3 animatedRotation =
            animation != null ? animation.rotation() : ModelVector3.ZERO;

        ModelVector3 scale =
            animation != null
                ? animation.scale()
                : new ModelVector3(1.0f, 1.0f, 1.0f);

        float units = BLOCKBENCH_UNITS_PER_BLOCK;

        // Apply animated translation.
        poseStack.translate(
            position.x() / units,
            position.y() / units,
            position.z() / units
        );

        // Move to the group pivot.
        poseStack.translate(
            origin.x() / units,
            origin.y() / units,
            origin.z() / units
        );

        // Combine base and animated rotation.
        poseStack.mulPose(
            Axis.ZP.rotationDegrees(baseRotation.z() + animatedRotation.z())
        );
        poseStack.mulPose(
            Axis.YP.rotationDegrees(baseRotation.y() + animatedRotation.y())
        );
        poseStack.mulPose(
            Axis.XP.rotationDegrees(baseRotation.x() + animatedRotation.x())
        );

        // Apply animated scale around the pivot.
        poseStack.scale(scale.x(), scale.y(), scale.z());

        // Move back from the pivot.
        poseStack.translate(
            -origin.x() / units,
            -origin.y() / units,
            -origin.z() / units
        );
    }

    private static void renderQuads(
        java.util.List<ModelQuad> quads,
        PoseStack poseStack,
        IntFunction<VertexConsumer> consumers,
        int packedLight,
        int packedOverlay
    ) {
        PoseStack.Pose pose = poseStack.last();

        for (ModelQuad quad : quads) {
            VertexConsumer consumer = Objects.requireNonNull(
                consumers.apply(quad.texture()),
                "No VertexConsumer supplied for texture index " + quad.texture()
            );

            renderQuad(quad, pose, consumer, packedLight, packedOverlay);
        }
    }

    /**
     * Renders hierarchical geometry with evaluated animation transforms.
     *
     * Animation transforms are matched to geometry nodes by UUID.
     * Nodes without an animation transform retain their base transforms.
     */
    public static void render(
        ModelGeometryHierarchy geometry,
        PoseStack poseStack,
        ModelTextureRegistration textures,
        MultiBufferSource buffers,
        boolean doubleSided,
        Map<String, AnimationTransform> animations,
        int packedLight,
        int packedOverlay
    ) {
        Objects.requireNonNull(
            animations,
            "Animation transforms cannot be null."
        );

        render(
            geometry,
            poseStack,
            textures,
            buffers,
            (index, location) ->
                doubleSided
                    ? RenderType.entityCutoutNoCull(location)
                    : RenderType.entityCutout(location),
            animations,
            packedLight,
            packedOverlay
        );
    }

    /**
     * Renders hierarchical geometry with animated transforms and
     * a configurable RenderType per texture.
     */
    public static void render(
        ModelGeometryHierarchy geometry,
        PoseStack poseStack,
        ModelTextureRegistration textures,
        MultiBufferSource buffers,
        BiFunction<Integer, ResourceLocation, RenderType> renderTypes,
        Map<String, AnimationTransform> animations,
        int packedLight,
        int packedOverlay
    ) {
        Objects.requireNonNull(geometry, "Geometry cannot be null.");
        Objects.requireNonNull(poseStack, "PoseStack cannot be null.");
        Objects.requireNonNull(textures, "Textures cannot be null.");
        Objects.requireNonNull(buffers, "Buffer source cannot be null.");
        Objects.requireNonNull(
            renderTypes,
            "Render type factory cannot be null."
        );
        Objects.requireNonNull(
            animations,
            "Animation transforms cannot be null."
        );

        Map<Integer, VertexConsumer> consumers = new HashMap<>();

        IntFunction<VertexConsumer> consumerProvider = index ->
            consumers.computeIfAbsent(index, textureIndex -> {
                ResourceLocation location = textures.require(textureIndex);

                RenderType renderType = Objects.requireNonNull(
                    renderTypes.apply(textureIndex, location),
                    "RenderType factory returned null for texture " +
                        textureIndex
                );

                return buffers.getBuffer(renderType);
            });

        renderHierarchy(
            geometry,
            poseStack,
            consumerProvider,
            animations,
            packedLight,
            packedOverlay
        );
    }
}
