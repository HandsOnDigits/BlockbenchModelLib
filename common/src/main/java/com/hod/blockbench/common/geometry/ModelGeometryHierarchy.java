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
    List<ModelQuad> rootQuads
) {
    public ModelGeometryHierarchy {
        roots = List.copyOf(roots);
        rootQuads = List.copyOf(rootQuads);
    }
}
