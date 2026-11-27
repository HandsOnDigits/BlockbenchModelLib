package com.hod.blockbench.common.geometry;

import com.hod.blockbench.model.ModelVector3;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Axis-aligned bounds of generated Blockbench geometry.
 *
 * Coordinates and dimensions are expressed in Blockbench model units.
 */
public record ModelBounds(ModelVector3 min, ModelVector3 max) {
    public ModelBounds {
        Objects.requireNonNull(min, "Minimum position cannot be null.");
        Objects.requireNonNull(max, "Maximum position cannot be null.");

        if (
            !Float.isFinite(min.x()) ||
            !Float.isFinite(min.y()) ||
            !Float.isFinite(min.z()) ||
            !Float.isFinite(max.x()) ||
            !Float.isFinite(max.y()) ||
            !Float.isFinite(max.z())
        ) {
            throw new IllegalArgumentException(
                "Model bounds must contain finite coordinates."
            );
        }

        if (min.x() > max.x() || min.y() > max.y() || min.z() > max.z()) {
            throw new IllegalArgumentException(
                "Minimum coordinates cannot exceed maximum coordinates."
            );
        }
    }

    /**
     * Calculates bounds from generated triangles.
     *
     * @return empty when the geometry contains no vertices
     */
    public static Optional<ModelBounds> fromTriangles(
        List<ModelTriangle> triangles
    ) {
        Objects.requireNonNull(triangles, "Triangles cannot be null.");

        float minX = Float.POSITIVE_INFINITY;
        float minY = Float.POSITIVE_INFINITY;
        float minZ = Float.POSITIVE_INFINITY;

        float maxX = Float.NEGATIVE_INFINITY;
        float maxY = Float.NEGATIVE_INFINITY;
        float maxZ = Float.NEGATIVE_INFINITY;

        for (ModelTriangle triangle : triangles) {
            for (ModelVertex vertex : triangle.vertices()) {
                ModelVector3 position = vertex.position();

                minX = Math.min(minX, position.x());
                minY = Math.min(minY, position.y());
                minZ = Math.min(minZ, position.z());

                maxX = Math.max(maxX, position.x());
                maxY = Math.max(maxY, position.y());
                maxZ = Math.max(maxZ, position.z());
            }
        }

        if (!Float.isFinite(minX)) {
            return Optional.empty();
        }

        return Optional.of(
            new ModelBounds(
                new ModelVector3(minX, minY, minZ),
                new ModelVector3(maxX, maxY, maxZ)
            )
        );
    }

    public ModelVector3 center() {
        return new ModelVector3(
            (min.x() + max.x()) * 0.5F,
            (min.y() + max.y()) * 0.5F,
            (min.z() + max.z()) * 0.5F
        );
    }

    public ModelVector3 size() {
        return new ModelVector3(width(), height(), depth());
    }

    public float width() {
        return max.x() - min.x();
    }

    public float height() {
        return max.y() - min.y();
    }

    public float depth() {
        return max.z() - min.z();
    }
}
