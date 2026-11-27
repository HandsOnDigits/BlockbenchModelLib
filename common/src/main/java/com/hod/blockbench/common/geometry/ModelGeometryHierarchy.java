package com.hod.blockbench.common.geometry;

import java.util.List;

/**
 * Hierarchical geometry for models that require runtime transformations.
 *
 * Geometry belonging directly to the model root is stored separately
 * from the outliner group hierarchy.
 */
public record ModelGeometryHierarchy(
    List<ModelGeometryNode> roots,
    List<ModelTriangle> rootTriangles
) {
    public ModelGeometryHierarchy {
        roots = List.copyOf(roots);
        rootTriangles = List.copyOf(rootTriangles);
    }
}
