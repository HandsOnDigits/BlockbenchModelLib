package com.hod.blockbench.animation;

import static org.junit.jupiter.api.Assertions.*;

import com.hod.blockbench.model.AnimationDataPoint;
import com.hod.blockbench.model.AnimationKeyframe;
import com.hod.blockbench.model.ModelAnimation;
import com.hod.blockbench.model.ModelAnimator;
import com.hod.blockbench.model.ModelVector3;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnimationEvaluatorTest {

    private static final String WHEEL_UUID = "wheel-uuid";

    private ModelAnimation createWheelAnimation() {
        ModelAnimator wheel = new ModelAnimator(
            WHEEL_UUID,
            "Wheel",
            "bone",
            false,
            false,
            List.of(
                keyframe("rotation", 0.0f, "0", "0", "0"),
                keyframe("rotation", 2.0f, "0", "-180", "0"),
                keyframe("rotation", 4.0f, "0", "-360", "0")
            )
        );

        return new ModelAnimation(
            "animation-uuid",
            "Spin",
            "loop",
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
            Map.of(WHEEL_UUID, wheel)
        );
    }

    private AnimationKeyframe keyframe(
        String channel,
        float time,
        String x,
        String y,
        String z
    ) {
        return new AnimationKeyframe(
            "keyframe-" + time,
            channel,
            time,
            "linear",
            0,
            List.of(new AnimationDataPoint(x, y, z))
        );
    }

    @Test
    void evaluatesWheelAtBeginning() {
        Map<String, AnimationTransform> result = AnimationEvaluator.evaluate(
            createWheelAnimation(),
            0.0f
        );

        AnimationTransform transform = result.get(WHEEL_UUID);

        assertNotNull(transform);
        assertEquals(0.0f, transform.rotation().y(), 0.001f);
    }

    @Test
    void interpolatesWheelAtTwoSeconds() {
        Map<String, AnimationTransform> result = AnimationEvaluator.evaluate(
            createWheelAnimation(),
            2.0f
        );

        AnimationTransform transform = result.get(WHEEL_UUID);

        assertNotNull(transform);
        assertEquals(-180.0f, transform.rotation().y(), 0.001f);
    }

    @Test
    void evaluatesWheelAtEnd() {
        Map<String, AnimationTransform> result = AnimationEvaluator.evaluate(
            createWheelAnimation(),
            4.0f
        );

        AnimationTransform transform = result.get(WHEEL_UUID);

        assertNotNull(transform);
        assertEquals(-360.0f, transform.rotation().y(), 0.001f);
    }

    @Test
    void interpolatesBetweenKeyframes() {
        Map<String, AnimationTransform> result = AnimationEvaluator.evaluate(
            createWheelAnimation(),
            1.0f
        );

        AnimationTransform transform = result.get(WHEEL_UUID);

        assertNotNull(transform);
        assertEquals(-90.0f, transform.rotation().y(), 0.001f);
    }

    @Test
    void clampsTimeBeforeAnimationStart() {
        Map<String, AnimationTransform> result = AnimationEvaluator.evaluate(
            createWheelAnimation(),
            -1.0f
        );

        AnimationTransform transform = result.get(WHEEL_UUID);

        assertNotNull(transform);
        assertEquals(0.0f, transform.rotation().y(), 0.001f);
    }

    @Test
    void clampsTimeAfterAnimationEnd() {
        Map<String, AnimationTransform> result = AnimationEvaluator.evaluate(
            createWheelAnimation(),
            5.0f
        );

        AnimationTransform transform = result.get(WHEEL_UUID);

        assertNotNull(transform);
        assertEquals(-360.0f, transform.rotation().y(), 0.001f);
    }

    @Test
    void missingChannelsUseIdentityValues() {
        Map<String, AnimationTransform> result = AnimationEvaluator.evaluate(
            createWheelAnimation(),
            1.0f
        );

        AnimationTransform transform = result.get(WHEEL_UUID);

        assertNotNull(transform);
        assertEquals(ModelVector3.ZERO, transform.position());
        assertEquals(new ModelVector3(1.0f, 1.0f, 1.0f), transform.scale());
    }

    @Test
    void stepInterpolationHoldsPreviousValue() {
        ModelAnimator animator = new ModelAnimator(
            WHEEL_UUID,
            "Wheel",
            "bone",
            false,
            false,
            List.of(
                new AnimationKeyframe(
                    "first",
                    "rotation",
                    0.0f,
                    "step",
                    0,
                    List.of(new AnimationDataPoint("0", "0", "0"))
                ),
                keyframe("rotation", 2.0f, "0", "-180", "0")
            )
        );

        ModelAnimation animation = new ModelAnimation(
            "animation-uuid",
            "Step Test",
            "once",
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
            Map.of(WHEEL_UUID, animator)
        );

        AnimationTransform transform = AnimationEvaluator.evaluate(
            animation,
            1.0f
        ).get(WHEEL_UUID);

        assertNotNull(transform);
        assertEquals(0.0f, transform.rotation().y(), 0.001f);
    }
}
