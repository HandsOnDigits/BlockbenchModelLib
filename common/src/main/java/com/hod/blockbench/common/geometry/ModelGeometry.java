package com.hod.blockbench.common.geometry;

import java.util.List;
import java.util.Optional;

public record ModelGeometry(List<ModelQuad> quads) {
    public ModelGeometry {
        quads = List.copyOf(quads);
    }

    /**
     * Returns the axis-aligned bounds of this geometry.
     *
     * Coordinates are expressed in Blockbench model units.
     */
    public Optional<ModelBounds> bounds() {
        return ModelBounds.from(quads);
    }
}
