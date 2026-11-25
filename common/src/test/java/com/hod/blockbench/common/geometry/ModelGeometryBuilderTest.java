
package com.hod.blockbench.common.geometry;

import com.hod.blockbench.model.*;
import com.hod.blockbench.loader.BlockbenchModelLoader;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.*;

class ModelGeometryBuilderTest {
    private static int[] expectedUvIndices(ModelFace.Direction direction) {
        return switch (direction) {
            case NORTH, SOUTH, WEST -> new int[]{1, 0, 3, 2};
            case EAST -> new int[]{0, 1, 2, 3};
            case UP, DOWN -> new int[]{0, 3, 2, 1};
        };
    }


    @Test
    void textileMachineFacesHaveCorrectWinding() throws Exception {
        try (InputStream input = getClass().getResourceAsStream(
                "/textile_machine.bbmodel"
        )) {
            assertNotNull(input);

            var model = BlockbenchModelLoader.load(input);
            var geometry = ModelGeometryBuilder.build(model);

            for (var quad : geometry.quads()) {
                var v0 = quad.vertices().get(0).position();
                var v1 = quad.vertices().get(1).position();
                var v2 = quad.vertices().get(2).position();

                var edge1 = v1.subtract(v0);
                var edge2 = v2.subtract(v0);

                float crossX = edge1.y() * edge2.z()
                        - edge1.z() * edge2.y();

                float crossY = edge1.z() * edge2.x()
                        - edge1.x() * edge2.z();

                float crossZ = edge1.x() * edge2.y()
                        - edge1.y() * edge2.x();

                var normal = quad.vertices().get(0).normal();

                float dot = crossX * normal.x()
                        + crossY * normal.y()
                        + crossZ * normal.z();

                assertTrue(
                        dot > 0.0001f,
                        "Quad winding does not match its normal: "
                                + quad.direction()
                );
            }
        }
    }

    @Test
    void mapsUvCoordinatesCorrectlyForAllFaceDirections() {
        ModelTexture texture = new ModelTexture(
                "texture-1",
                "test",
                16,
                16,
                16,
                16,
                1,
                "custom",
                "",
                false,
                "",
                null
        );

        ModelFace.ModelUv uv = new ModelFace.ModelUv(
                2, 4, 10, 12
        );

        Map<ModelFace.Direction, ModelFace> faces = new java.util.EnumMap<>(
                ModelFace.Direction.class
        );

        for (ModelFace.Direction direction : ModelFace.Direction.values()) {
            faces.put(
                    direction,
                    new ModelFace(direction, uv, 0, 0)
            );
        }

        ModelCube cube = new ModelCube(
                "cube-1",
                "test-cube",
                true,
                new ModelVector3(1, 2, 3),
                new ModelVector3(5, 8, 11),
                ModelVector3.ZERO,
                ModelVector3.ZERO,
                0,
                faces
        );

        BlockbenchModel model = new BlockbenchModel(
                "test",
                "test",
                null,
                List.of(cube),
                List.of(),
                List.of(texture),
                ""
        );

        ModelGeometry geometry = ModelGeometryBuilder.build(model);

        List<ModelUvTransform.UvPoint> uvCorners = List.of(
                new ModelUvTransform.UvPoint(0.125f, 0.25f),
                new ModelUvTransform.UvPoint(0.625f, 0.25f),
                new ModelUvTransform.UvPoint(0.625f, 0.75f),
                new ModelUvTransform.UvPoint(0.125f, 0.75f)
        );

        assertEquals(6, geometry.quads().size());

        for (int faceIndex = 0;
             faceIndex < ModelFace.Direction.values().length;
             faceIndex++) {

            ModelFace.Direction direction =
                    ModelFace.Direction.values()[faceIndex];

            ModelQuad quad = geometry.quads().get(faceIndex);

            assertEquals(direction, quad.direction());

            int[] indices = expectedUvIndices(direction);

            for (int vertexIndex = 0; vertexIndex < 4; vertexIndex++) {
                ModelVertex vertex = quad.vertices().get(vertexIndex);

                ModelUvTransform.UvPoint expected =
                        uvCorners.get(indices[vertexIndex]);

                assertEquals(
                        expected.u(),
                        vertex.u(),
                        0.0001f,
                        direction + " vertex " + vertexIndex + " U"
                );

                assertEquals(
                        expected.v(),
                        vertex.v(),
                        0.0001f,
                        direction + " vertex " + vertexIndex + " V"
                );
            }
        }
    }
}
