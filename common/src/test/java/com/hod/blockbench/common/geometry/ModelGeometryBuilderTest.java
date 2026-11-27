package com.hod.blockbench.common.geometry;

import static org.junit.jupiter.api.Assertions.*;

import com.hod.blockbench.loader.BlockbenchModelLoader;
import com.hod.blockbench.model.BlockbenchModel;
import com.hod.blockbench.model.ModelElement;
import com.hod.blockbench.model.ModelFaceVertex;
import com.hod.blockbench.model.ModelPolygonFace;
import com.hod.blockbench.model.ModelTexture;
import com.hod.blockbench.model.ModelVector3;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class ModelGeometryBuilderTest {

    private static ModelTexture testTexture() {
        return new ModelTexture(
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
    }

    private static BlockbenchModel modelWithPolygon(
        List<ModelVector3> positions,
        List<ModelFaceVertex> corners
    ) {
        Map<String, ModelVector3> vertices = new LinkedHashMap<>();

        for (int i = 0; i < positions.size(); i++) {
            vertices.put("vertex-" + i, positions.get(i));
        }

        ModelElement element = new ModelElement(
            "element-1",
            "test-element",
            "mesh",
            true,
            ModelVector3.ZERO,
            ModelVector3.ZERO,
            vertices,
            List.of(new ModelPolygonFace("face-1", 0, corners)),
            ""
        );

        return new BlockbenchModel(
            "test",
            "test",
            null,
            List.of(element),
            List.of(),
            List.of(testTexture()),
            List.of(),
            ""
        );
    }

    private static List<ModelFaceVertex> corners(int count) {
        List<ModelFaceVertex> result = new java.util.ArrayList<>();

        for (int i = 0; i < count; i++) {
            result.add(new ModelFaceVertex("vertex-" + i, i, i + 1));
        }

        return List.copyOf(result);
    }

    @Test
    void textileMachineTrianglesHaveCorrectWinding() throws Exception {
        try (
            InputStream input = getClass().getResourceAsStream(
                "/textile_machine.bbmodel"
            )
        ) {
            assertNotNull(input);

            BlockbenchModel model = BlockbenchModelLoader.load(input);
            ModelGeometry geometry = ModelGeometryBuilder.build(model);

            assertFalse(
                geometry.triangles().isEmpty(),
                "The textile machine should produce geometry."
            );

            for (ModelTriangle triangle : geometry.triangles()) {
                ModelVector3 v0 = triangle.vertices().get(0).position();
                ModelVector3 v1 = triangle.vertices().get(1).position();
                ModelVector3 v2 = triangle.vertices().get(2).position();

                ModelVector3 edge1 = v1.subtract(v0);
                ModelVector3 edge2 = v2.subtract(v0);

                float crossX = edge1.y() * edge2.z() - edge1.z() * edge2.y();

                float crossY = edge1.z() * edge2.x() - edge1.x() * edge2.z();

                float crossZ = edge1.x() * edge2.y() - edge1.y() * edge2.x();

                ModelVector3 normal = triangle.vertices().get(0).normal();

                float dot =
                    crossX * normal.x() +
                    crossY * normal.y() +
                    crossZ * normal.z();

                assertTrue(
                    dot > 0.0001f,
                    "Triangle winding does not match its normal."
                );
            }
        }
    }

    @Test
    void preservesPerCornerUvCoordinates() {
        List<ModelVector3> positions = List.of(
            new ModelVector3(0, 0, 0),
            new ModelVector3(4, 0, 0),
            new ModelVector3(4, 4, 0),
            new ModelVector3(0, 4, 0)
        );

        List<ModelFaceVertex> corners = List.of(
            new ModelFaceVertex("vertex-0", 2, 4),
            new ModelFaceVertex("vertex-1", 10, 4),
            new ModelFaceVertex("vertex-2", 10, 12),
            new ModelFaceVertex("vertex-3", 2, 12)
        );

        ModelGeometry geometry = ModelGeometryBuilder.build(
            modelWithPolygon(positions, corners)
        );

        assertEquals(2, geometry.triangles().size());

        Set<String> actualUvs = geometry
            .triangles()
            .stream()
            .flatMap(triangle -> triangle.vertices().stream())
            .map(vertex ->
                String.format(
                    java.util.Locale.ROOT,
                    "%.4f,%.4f",
                    vertex.u(),
                    vertex.v()
                )
            )
            .collect(Collectors.toSet());

        assertEquals(
            Set.of(
                "0.1250,0.2500",
                "0.6250,0.2500",
                "0.6250,0.7500",
                "0.1250,0.7500"
            ),
            actualUvs
        );
    }

    @Test
    void triangulatesConcavePolygon() {
        // A simple concave polygon shaped like an arrow/notched square.
        List<ModelVector3> positions = List.of(
            new ModelVector3(0, 0, 0),
            new ModelVector3(4, 0, 0),
            new ModelVector3(4, 4, 0),
            new ModelVector3(2, 2, 0),
            new ModelVector3(0, 4, 0)
        );

        ModelGeometry geometry = ModelGeometryBuilder.build(
            modelWithPolygon(positions, corners(positions.size()))
        );

        assertEquals(
            3,
            geometry.triangles().size(),
            "A simple five-vertex polygon should produce three triangles."
        );

        for (ModelTriangle triangle : geometry.triangles()) {
            ModelVector3 v0 = triangle.vertices().get(0).position();
            ModelVector3 v1 = triangle.vertices().get(1).position();
            ModelVector3 v2 = triangle.vertices().get(2).position();

            float crossZ =
                (v1.x() - v0.x()) * (v2.y() - v0.y()) -
                (v1.y() - v0.y()) * (v2.x() - v0.x());

            assertTrue(
                crossZ > 0.0001f,
                "Concave polygon produced a degenerate or reversed triangle."
            );
        }
    }
}
