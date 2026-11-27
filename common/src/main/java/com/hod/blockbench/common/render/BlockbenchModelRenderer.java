package com.hod.blockbench.common.render;

import com.hod.blockbench.animation.AnimationTransform;
import com.hod.blockbench.common.geometry.ModelGeometry;
import com.hod.blockbench.common.geometry.ModelGeometryHierarchy;
import com.hod.blockbench.common.geometry.ModelGeometryNode;
import com.hod.blockbench.common.geometry.ModelTriangle;
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

    // ---------------------------------------------------------------------
    // Flat geometry
    // ---------------------------------------------------------------------

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

        renderTriangles(
            geometry.triangles(),
            poseStack,
            consumerProvider,
            packedLight,
            packedOverlay
        );
    }

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

        renderTriangles(
            geometry.triangles(),
            poseStack,
            consumers,
            packedLight,
            packedOverlay
        );
    }

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

    // ---------------------------------------------------------------------
    // Hierarchical geometry
    // ---------------------------------------------------------------------

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

    private static void renderHierarchy(
        ModelGeometryHierarchy geometry,
        PoseStack poseStack,
        IntFunction<VertexConsumer> consumers,
        Map<String, AnimationTransform> animations,
        int packedLight,
        int packedOverlay
    ) {
        renderTriangles(
            geometry.rootTriangles(),
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

            renderTriangles(
                node.triangles(),
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

    // ---------------------------------------------------------------------
    // Animation transforms
    // ---------------------------------------------------------------------
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

        // 1. Move to bone pivot in parent space + apply local bone translation
        poseStack.translate(
            origin.x() / units,
            origin.y() / units,
            origin.z() / units
        );

        // 2. Apply bone rotation (local X -> Y -> Z via post-multiplication)
        poseStack.mulPose(
            Axis.ZP.rotationDegrees(baseRotation.z() + animatedRotation.z())
        );
        poseStack.mulPose(
            Axis.YP.rotationDegrees(baseRotation.y() + animatedRotation.y())
        );
        poseStack.mulPose(
            Axis.XP.rotationDegrees(baseRotation.x() + animatedRotation.x())
        );

        // 3. Apply bone position offset in the bone's rotated frame
        poseStack.translate(
            position.x() / units,
            position.y() / units,
            position.z() / units
        );

        // 4. Scale around pivot, then return from pivot
        poseStack.scale(scale.x(), scale.y(), scale.z());
        poseStack.translate(
            -origin.x() / units,
            -origin.y() / units,
            -origin.z() / units
        );
    }

    // ---------------------------------------------------------------------
    // Triangle submission
    // ---------------------------------------------------------------------

    private static void renderTriangles(
        java.util.List<ModelTriangle> triangles,
        PoseStack poseStack,
        IntFunction<VertexConsumer> consumers,
        int packedLight,
        int packedOverlay
    ) {
        PoseStack.Pose pose = poseStack.last();

        for (ModelTriangle triangle : triangles) {
            VertexConsumer consumer = Objects.requireNonNull(
                consumers.apply(triangle.texture()),
                "No VertexConsumer supplied for texture index " +
                    triangle.texture()
            );

            renderTriangle(
                triangle,
                pose,
                consumer,
                packedLight,
                packedOverlay
            );
        }
    }

    /**
     * Minecraft's entity cutout render types use quad vertices.
     *
     * A triangle is submitted as a degenerate quad:
     * v0, v1, v2, v2.
     *
     * The duplicated final vertex creates a zero-area second triangle,
     * leaving the intended triangle visible.
     */
    private static void renderTriangle(
        ModelTriangle triangle,
        PoseStack.Pose pose,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay
    ) {
        for (ModelVertex vertex : triangle.vertices()) {
            renderVertex(vertex, pose, consumer, packedLight, packedOverlay);
        }

        renderVertex(
            triangle.vertices().get(2),
            pose,
            consumer,
            packedLight,
            packedOverlay
        );
    }

    private static void renderVertex(
        ModelVertex vertex,
        PoseStack.Pose pose,
        VertexConsumer consumer,
        int packedLight,
        int packedOverlay
    ) {
        ModelVector3 position = vertex.position();
        ModelVector3 normal = vertex.normal();

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
