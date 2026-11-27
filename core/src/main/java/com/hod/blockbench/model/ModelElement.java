package com.hod.blockbench.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Unified representation of a Blockbench element.
 *
 * Supports cube-derived geometry, free-form meshes, and elements
 * whose original Blockbench properties are not yet interpreted.
 *
 * All geometry is represented by vertices and polygon faces.
 */
public record ModelElement(
    String uuid,
    String name,
    String originalType,
    boolean visible,
    ModelVector3 origin,
    ModelVector3 rotation,
    Map<String, ModelVector3> vertices,
    List<ModelPolygonFace> faces,
    String rawJson
) {
    public ModelElement {
        vertices =
            vertices == null
                ? Map.of()
                : Collections.unmodifiableMap(new LinkedHashMap<>(vertices));

        faces = faces == null ? List.of() : List.copyOf(faces);

        origin = origin != null ? origin : ModelVector3.ZERO;
        rotation = rotation != null ? rotation : ModelVector3.ZERO;
        rawJson = rawJson != null ? rawJson : "";
    }

    public ModelVector3 findVertex(String uuid) {
        return vertices.get(uuid);
    }

    public boolean hasGeometry() {
        return !vertices.isEmpty() && !faces.isEmpty();
    }

    public int faceCount() {
        return faces.size();
    }
}
