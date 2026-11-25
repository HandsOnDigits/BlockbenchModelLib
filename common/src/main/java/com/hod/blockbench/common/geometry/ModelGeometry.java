package com.hod.blockbench.common.geometry;

import java.util.List;

public record ModelGeometry(
        List<ModelQuad> quads
) {
    public ModelGeometry {
        quads = List.copyOf(quads);
    }
}