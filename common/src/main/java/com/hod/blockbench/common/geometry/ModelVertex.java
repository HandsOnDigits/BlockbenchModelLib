package com.hod.blockbench.common.geometry;

import com.hod.blockbench.model.ModelVector3;

public record ModelVertex(
        ModelVector3 position,
        float u,
        float v,
        ModelVector3 normal
) {
}