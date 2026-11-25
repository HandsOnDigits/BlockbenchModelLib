package com.hod.blockbench.model;

import java.util.List;

public record ModelBone(
        String uuid,
        String name,
        ModelVector3 origin,
        ModelVector3 rotation,
        List<String> cubeUuids,
        List<ModelBone> children
) {
    public ModelBone {
        cubeUuids = List.copyOf(cubeUuids);
        children = List.copyOf(children);
    }
}