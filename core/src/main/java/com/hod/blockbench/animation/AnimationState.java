package com.hod.blockbench.animation;

import com.hod.blockbench.model.ModelAnimation;
import java.util.Map;

/**
 * Immutable snapshot of an animation's playback state.
 */
public record AnimationState(
    ModelAnimation animation,
    float time,
    float speed,
    AnimationPlaybackStatus status
) {
    public AnimationState {
        if (time < 0.0f || !Float.isFinite(time)) {
            throw new IllegalArgumentException(
                "Animation time must be finite and non-negative."
            );
        }

        if (!Float.isFinite(speed) || speed <= 0.0f) {
            throw new IllegalArgumentException(
                "Animation speed must be finite and greater than zero."
            );
        }
    }

    public boolean isPlaying() {
        return status == AnimationPlaybackStatus.PLAYING;
    }

    public boolean isPaused() {
        return status == AnimationPlaybackStatus.PAUSED;
    }

    public boolean isFinished() {
        return status == AnimationPlaybackStatus.FINISHED;
    }

    /**
     * Evaluates the animation at this state's current time.
     */
    public Map<String, AnimationTransform> evaluate() {
        if (animation == null) {
            return Map.of();
        }

        return AnimationEvaluator.evaluate(animation, time);
    }
}
