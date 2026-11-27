package com.hod.blockbench.model;

import java.util.List;

/**
 * A polygon face belonging to a ModelElement.
 *
 * Vertices are ordered around the polygon and each corner has
 * its own UV coordinates.
 */
public record ModelPolygonFace(
    String uuid,
    int texture,
    List<ModelFaceVertex> vertices
) {
    public ModelPolygonFace {
        vertices = List.copyOf(vertices);

        if (vertices.size() < 3) {
            throw new IllegalArgumentException(
                "A polygon face must contain at least three vertices."
            );
        }
    }
}
