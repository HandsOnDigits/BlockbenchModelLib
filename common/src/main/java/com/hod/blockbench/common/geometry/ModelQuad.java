package com.hod.blockbench.common.geometry;

import com.hod.blockbench.model.ModelFace;

import java.util.List;

public record ModelQuad(
        ModelFace.Direction direction,
        int texture,
        List<ModelVertex> vertices
) {
    public ModelQuad {
        if (vertices == null || vertices.size() != 4) {
            throw new IllegalArgumentException(
                    "A model quad must contain exactly four vertices."
            );
        }

        vertices = List.copyOf(vertices);
    }
}