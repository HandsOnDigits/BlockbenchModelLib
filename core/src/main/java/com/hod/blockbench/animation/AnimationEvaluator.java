package com.hod.blockbench.animation;

import com.hod.blockbench.model.AnimationDataPoint;
import com.hod.blockbench.model.AnimationKeyframe;
import com.hod.blockbench.model.ModelAnimation;
import com.hod.blockbench.model.ModelAnimator;
import com.hod.blockbench.model.ModelVector3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Evaluates Blockbench animation keyframes.
 *
 * This class has no Minecraft dependencies.
 */
public final class AnimationEvaluator {

    private AnimationEvaluator() {}

    /**
     * Evaluates an animation at the specified time.
     *
     * Time is expressed in seconds and is clamped to the animation length.
     *
     * @return immutable map of target outliner UUIDs to evaluated transforms
     */
    public static Map<String, AnimationTransform> evaluate(
        ModelAnimation animation,
        float time
    ) {
        Objects.requireNonNull(animation, "Animation cannot be null.");

        if (!Float.isFinite(time)) {
            throw new IllegalArgumentException(
                "Animation time must be finite."
            );
        }

        float evaluationTime = Math.max(
            0.0f,
            Math.min(time, Math.max(0.0f, animation.length()))
        );

        Map<String, AnimationTransform> result = new HashMap<>();

        for (ModelAnimator animator : animation.animators().values()) {
            if (animator.targetUuid() == null) {
                continue;
            }

            result.put(
                animator.targetUuid(),
                evaluateAnimator(animator, evaluationTime)
            );
        }

        return Map.copyOf(result);
    }

    /**
     * Evaluates all supported channels of one animator.
     */
    private static AnimationTransform evaluateAnimator(
        ModelAnimator animator,
        float time
    ) {
        ModelVector3 position = evaluateChannel(
            animator.keyframes(),
            "position",
            time,
            ModelVector3.ZERO
        );

        ModelVector3 rotation = evaluateChannel(
            animator.keyframes(),
            "rotation",
            time,
            ModelVector3.ZERO
        );

        ModelVector3 scale = evaluateChannel(
            animator.keyframes(),
            "scale",
            time,
            new ModelVector3(1.0f, 1.0f, 1.0f)
        );

        return new AnimationTransform(position, rotation, scale);
    }

    /**
     * Evaluates one channel independently.
     */
    private static ModelVector3 evaluateChannel(
        List<AnimationKeyframe> keyframes,
        String channel,
        float time,
        ModelVector3 defaultValue
    ) {
        List<AnimationKeyframe> matching = new ArrayList<>();

        for (AnimationKeyframe keyframe : keyframes) {
            if (
                channel.equalsIgnoreCase(keyframe.channel()) &&
                keyframe.firstDataPoint() != null &&
                Float.isFinite(keyframe.time())
            ) {
                matching.add(keyframe);
            }
        }

        if (matching.isEmpty()) {
            return defaultValue;
        }

        matching.sort(Comparator.comparingDouble(AnimationKeyframe::time));

        AnimationKeyframe first = matching.get(0);
        AnimationKeyframe last = matching.get(matching.size() - 1);

        if (time <= first.time()) {
            return vector(first.firstDataPoint());
        }

        if (time >= last.time()) {
            return vector(last.firstDataPoint());
        }

        for (int i = 0; i < matching.size() - 1; i++) {
            AnimationKeyframe current = matching.get(i);
            AnimationKeyframe next = matching.get(i + 1);

            if (time < current.time() || time > next.time()) {
                continue;
            }

            float duration = next.time() - current.time();

            if (duration <= 0.0f) {
                return vector(next.firstDataPoint());
            }

            if ("step".equalsIgnoreCase(current.interpolation())) {
                return vector(current.firstDataPoint());
            }

            float amount = (time - current.time()) / duration;

            return interpolate(
                vector(current.firstDataPoint()),
                vector(next.firstDataPoint()),
                amount
            );
        }

        return vector(last.firstDataPoint());
    }

    private static ModelVector3 vector(AnimationDataPoint point) {
        return point.toVector3();
    }

    private static ModelVector3 interpolate(
        ModelVector3 from,
        ModelVector3 to,
        float amount
    ) {
        return new ModelVector3(
            lerp(from.x(), to.x(), amount),
            lerp(from.y(), to.y(), amount),
            lerp(from.z(), to.z(), amount)
        );
    }

    private static float lerp(float from, float to, float amount) {
        return from + (to - from) * amount;
    }
}
