package com.hod.blockbench.animation;

import com.hod.blockbench.model.ModelAnimation;
import java.util.Objects;

/**
 * Controls playback of a single Blockbench animation.
 *
 * Time is expressed in seconds.
 *
 * This class is independent of Minecraft's tick system.
 */
public final class AnimationController {

    private ModelAnimation animation;

    private float time;
    private float speed = 1.0f;

    private AnimationPlaybackStatus status = AnimationPlaybackStatus.STOPPED;

    /**
     * Starts an animation from the beginning.
     */
    public void play(ModelAnimation animation) {
        this.animation = Objects.requireNonNull(
            animation,
            "Animation cannot be null."
        );

        time = 0.0f;
        status = AnimationPlaybackStatus.PLAYING;
    }

    /**
     * Pauses the active animation.
     */
    public void pause() {
        if (status == AnimationPlaybackStatus.PLAYING) {
            status = AnimationPlaybackStatus.PAUSED;
        }
    }

    /**
     * Resumes a paused animation.
     */
    public void resume() {
        if (status == AnimationPlaybackStatus.PAUSED) {
            status = AnimationPlaybackStatus.PLAYING;
        }
    }

    /**
     * Stops playback and resets the animation time.
     *
     * The selected animation remains available for inspection.
     */
    public void stop() {
        time = 0.0f;
        status = AnimationPlaybackStatus.STOPPED;
    }

    /**
     * Advances playback by the supplied elapsed time.
     *
     * @param deltaSeconds elapsed real or simulation time in seconds
     */
    public void update(float deltaSeconds) {
        if (!Float.isFinite(deltaSeconds) || deltaSeconds < 0.0f) {
            throw new IllegalArgumentException(
                "Delta time must be finite and non-negative."
            );
        }

        if (status != AnimationPlaybackStatus.PLAYING || animation == null) {
            return;
        }

        float length = animation.length();

        if (!Float.isFinite(length) || length <= 0.0f) {
            time = 0.0f;
            status = AnimationPlaybackStatus.FINISHED;
            return;
        }

        time += deltaSeconds * speed;

        if (animation.isLooping()) {
            time %= length;
            return;
        }

        if (time >= length) {
            time = length;
            status = AnimationPlaybackStatus.FINISHED;
        }
    }

    /**
     * Sets the current animation time.
     *
     * Seeking does not change the playback status.
     */
    public void seek(float time) {
        if (!Float.isFinite(time)) {
            throw new IllegalArgumentException(
                "Animation time must be finite."
            );
        }

        if (animation == null) {
            throw new IllegalStateException(
                "Cannot seek without an active animation."
            );
        }

        float length = Math.max(0.0f, animation.length());

        if (animation.isLooping() && length > 0.0f) {
            this.time = ((time % length) + length) % length;
        } else {
            this.time = Math.max(0.0f, Math.min(time, length));
        }

        if (status == AnimationPlaybackStatus.FINISHED && this.time < length) {
            status = AnimationPlaybackStatus.PAUSED;
        }
    }

    /**
     * Changes the playback speed.
     *
     * 1.0 = normal speed
     * 2.0 = double speed
     * 0.5 = half speed
     */
    public void setSpeed(float speed) {
        if (!Float.isFinite(speed) || speed <= 0.0f) {
            throw new IllegalArgumentException(
                "Animation speed must be finite and greater than zero."
            );
        }

        this.speed = speed;
    }

    public float time() {
        return time;
    }

    public float speed() {
        return speed;
    }

    public ModelAnimation animation() {
        return animation;
    }

    public AnimationPlaybackStatus status() {
        return status;
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
     * Returns an immutable snapshot of the current playback state.
     */
    public AnimationState state() {
        return new AnimationState(animation, time, speed, status);
    }

    /**
     * Evaluates the currently selected animation.
     */
    public AnimationState evaluate() {
        return state();
    }
}
