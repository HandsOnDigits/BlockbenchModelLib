package com.hod.blockbench.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public record ModelAnimation(
    String uuid,
    String name,
    String loop,
    boolean override,
    float length,
    int snapping,
    boolean selected,
    String groupName,
    int scope,
    String animTimeUpdate,
    String blendWeight,
    String startDelay,
    String loopDelay,
    Map<String, ModelAnimator> animators
) {
    public ModelAnimation {
        animators = Collections.unmodifiableMap(new LinkedHashMap<>(animators));
    }

    /**
     * Finds an animator by its target outliner UUID.
     */
    public ModelAnimator findAnimator(String targetUuid) {
        return targetUuid == null ? null : animators.get(targetUuid);
    }

    /**
     * Returns the animator associated with a named group.
     * Names are not guaranteed to be unique.
     */
    public java.util.List<ModelAnimator> findAnimatorsByName(String name) {
        if (name == null) {
            return java.util.List.of();
        }

        return animators
            .values()
            .stream()
            .filter(animator -> name.equals(animator.name()))
            .toList();
    }

    public boolean isLooping() {
        return "loop".equalsIgnoreCase(loop);
    }
}
