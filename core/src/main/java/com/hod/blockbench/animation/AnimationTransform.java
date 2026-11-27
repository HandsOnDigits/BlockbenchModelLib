package com.hod.blockbench.animation;

import com.hod.blockbench.model.ModelVector3;

/**
 * Evaluated animation values for one model node.
 *
 * Position and rotation are offsets from the node's base transform.
 * Scale is a multiplier, where (1, 1, 1) represents no scaling.
 */
public record AnimationTransform(
    ModelVector3 position,
    ModelVector3 rotation,
    ModelVector3 scale
) {
    public static final AnimationTransform IDENTITY = new AnimationTransform(
        ModelVector3.ZERO,
        ModelVector3.ZERO,
        new ModelVector3(1.0f, 1.0f, 1.0f)
    );
}
