package com.hod.blockbench.common.geometry;

import java.util.List;

/**
 * A triangulated polygon face.
 *
 * Triangles are the generated geometry primitive. Original polygon
 * topology and face corner data remain in the parsed Blockbench model.
 */
public record ModelTriangle(int texture, List<ModelVertex> vertices) {
    public ModelTriangle {
        if (vertices == null || vertices.size() != 3) {
            throw new IllegalArgumentException(
                "A model triangle must contain exactly three vertices."
            );
        }

        vertices = List.copyOf(vertices);
    }
}
