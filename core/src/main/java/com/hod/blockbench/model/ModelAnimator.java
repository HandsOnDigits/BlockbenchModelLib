package com.hod.blockbench.model;

import java.util.List;

public record ModelAnimator(
    String targetUuid,
    String name,
    String type,
    boolean rotationGlobal,
    boolean quaternionInterpolation,
    List<AnimationKeyframe> keyframes
) {
    public ModelAnimator {
        keyframes = List.copyOf(keyframes);
    }
}
