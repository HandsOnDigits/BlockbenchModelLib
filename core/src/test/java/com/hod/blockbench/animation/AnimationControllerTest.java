package com.hod.blockbench.animation;

import static org.junit.jupiter.api.Assertions.*;

import com.hod.blockbench.model.ModelAnimation;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AnimationControllerTest {

    private AnimationController controller;

    private ModelAnimation loopingAnimation;
    private ModelAnimation onceAnimation;

    @BeforeEach
    void setUp() {
        controller = new AnimationController();

        loopingAnimation = createAnimation("loop");
        onceAnimation = createAnimation("once");
    }

    private ModelAnimation createAnimation(String loop) {
        return new ModelAnimation(
            "animation-uuid",
            "Test Animation",
            loop,
            false,
            4.0f,
            24,
            false,
            null,
            0,
            null,
            null,
            null,
            null,
            Map.of()
        );
    }

    @Test
    void playStartsFromBeginning() {
        controller.play(loopingAnimation);

        assertEquals(0.0f, controller.time(), 0.001f);
        assertEquals(AnimationPlaybackStatus.PLAYING, controller.status());
        assertTrue(controller.isPlaying());
    }

    @Test
    void updateAdvancesAnimationTime() {
        controller.play(loopingAnimation);
        controller.update(1.5f);

        assertEquals(1.5f, controller.time(), 0.001f);
    }

    @Test
    void pausePreventsTimeAdvancement() {
        controller.play(loopingAnimation);
        controller.update(1.0f);
        controller.pause();

        controller.update(2.0f);

        assertEquals(1.0f, controller.time(), 0.001f);
        assertEquals(AnimationPlaybackStatus.PAUSED, controller.status());
    }

    @Test
    void resumeContinuesPlayback() {
        controller.play(loopingAnimation);
        controller.update(1.0f);
        controller.pause();
        controller.resume();
        controller.update(1.0f);

        assertEquals(2.0f, controller.time(), 0.001f);
        assertTrue(controller.isPlaying());
    }

    @Test
    void stopResetsTime() {
        controller.play(loopingAnimation);
        controller.update(2.0f);
        controller.stop();

        assertEquals(0.0f, controller.time(), 0.001f);
        assertEquals(AnimationPlaybackStatus.STOPPED, controller.status());
    }

    @Test
    void seekChangesAnimationTime() {
        controller.play(loopingAnimation);
        controller.seek(2.5f);

        assertEquals(2.5f, controller.time(), 0.001f);
        assertTrue(controller.isPlaying());
    }

    @Test
    void playbackSpeedChangesTimeAdvancement() {
        controller.play(loopingAnimation);
        controller.setSpeed(2.0f);
        controller.update(1.0f);

        assertEquals(2.0f, controller.time(), 0.001f);
    }

    @Test
    void loopingAnimationWrapsAround() {
        controller.play(loopingAnimation);
        controller.update(4.5f);

        assertEquals(0.5f, controller.time(), 0.001f);
        assertTrue(controller.isPlaying());
    }

    @Test
    void loopingAnimationWrapsAtExactLength() {
        controller.play(loopingAnimation);
        controller.update(4.0f);

        assertEquals(0.0f, controller.time(), 0.001f);
        assertTrue(controller.isPlaying());
    }

    @Test
    void onceAnimationFinishesAtEnd() {
        controller.play(onceAnimation);
        controller.update(5.0f);

        assertEquals(4.0f, controller.time(), 0.001f);
        assertEquals(AnimationPlaybackStatus.FINISHED, controller.status());
        assertTrue(controller.isFinished());
    }

    @Test
    void finishedAnimationDoesNotAdvanceFurther() {
        controller.play(onceAnimation);
        controller.update(5.0f);
        controller.update(1.0f);

        assertEquals(4.0f, controller.time(), 0.001f);
        assertTrue(controller.isFinished());
    }

    @Test
    void playingNewAnimationResetsPreviousPlayback() {
        controller.play(onceAnimation);
        controller.update(2.0f);

        controller.play(loopingAnimation);

        assertEquals(0.0f, controller.time(), 0.001f);
        assertSame(loopingAnimation, controller.animation());
        assertTrue(controller.isPlaying());
    }

    @Test
    void stateContainsCurrentPlaybackInformation() {
        controller.play(loopingAnimation);
        controller.setSpeed(1.5f);
        controller.update(1.0f);

        AnimationState state = controller.state();

        assertSame(loopingAnimation, state.animation());
        assertEquals(1.5f, state.time(), 0.001f);
        assertEquals(1.5f, state.speed(), 0.001f);
        assertEquals(AnimationPlaybackStatus.PLAYING, state.status());
    }

    @Test
    void invalidDeltaTimeIsRejected() {
        controller.play(loopingAnimation);

        assertThrows(IllegalArgumentException.class, () ->
            controller.update(-1.0f)
        );

        assertThrows(IllegalArgumentException.class, () ->
            controller.update(Float.NaN)
        );
    }

    @Test
    void invalidPlaybackSpeedIsRejected() {
        assertThrows(IllegalArgumentException.class, () ->
            controller.setSpeed(0.0f)
        );

        assertThrows(IllegalArgumentException.class, () ->
            controller.setSpeed(-1.0f)
        );
    }
}
