package com.hod.blockbench.common.geometry;

import com.hod.blockbench.model.ModelVector3;
import java.util.List;

/**
 * A node in a hierarchical Blockbench model.
 *
 * Group transforms are retained for runtime rendering rather than
 * being baked into the node's geometry.
 */
public record ModelGeometryNode(
    String uuid,
    String name,
    ModelVector3 origin,
    ModelVector3 rotation,
    List<ModelTriangle> triangles,
    List<ModelGeometryNode> children
) {
    public ModelGeometryNode {
        triangles = List.copyOf(triangles);
        children = List.copyOf(children);
    }
}
