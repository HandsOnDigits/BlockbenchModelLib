package com.hod.blockbench.common.render;

import static org.junit.jupiter.api.Assertions.*;

import com.hod.blockbench.animation.AnimationController;
import com.hod.blockbench.animation.AnimationEvaluator;
import com.hod.blockbench.animation.AnimationPlaybackStatus;
import com.hod.blockbench.animation.AnimationTransform;
import com.hod.blockbench.common.geometry.ModelGeometryBuilder;
import com.hod.blockbench.common.geometry.ModelGeometryHierarchy;
import com.hod.blockbench.common.geometry.ModelGeometryNode;
import com.hod.blockbench.loader.BlockbenchModelLoader;
import com.hod.blockbench.model.BlockbenchModel;
import com.hod.blockbench.model.ModelAnimation;
import java.io.InputStream;
import java.util.Map;
import org.junit.jupiter.api.Test;

class SpinningWheelAnimationTest {

    private static final String MODEL_RESOURCE = "/spinning_wheel.bbmodel";

    private static final String FRAME_UUID =
        "3e2656fc-fb21-7437-63c6-8f9a19f45ce0";

    private static final String WHEEL_UUID =
        "e5e6cef6-a49f-7ebc-0302-1d635d6b6d41";

    private static final String ROLL_UUID =
        "0226d058-7e2d-639f-067f-4e6b961ae6a7";

    private BlockbenchModel loadModel() throws Exception {
        InputStream input = getClass().getResourceAsStream(MODEL_RESOURCE);

        assertNotNull(input, "Spinning wheel test model was not found.");

        try (input) {
            return BlockbenchModelLoader.load(input);
        }
    }

    private ModelAnimation loadSpinAnimation() throws Exception {
        BlockbenchModel model = loadModel();

        ModelAnimation animation = model
            .findAnimationsByName("Spin")
            .stream()
            .findFirst()
            .orElse(null);

        assertNotNull(animation, "Spin animation was not parsed.");

        return animation;
    }

    @Test
    void loadsActualSpinningWheelModel() throws Exception {
        BlockbenchModel model = loadModel();

        assertEquals("spinning_wheel", model.name());
        assertEquals(1, model.animations().size());

        ModelAnimation animation = model
            .findAnimationsByName("Spin")
            .stream()
            .findFirst()
            .orElseThrow();

        assertEquals(4.0f, animation.length(), 0.001f);
        assertTrue(animation.isLooping());
        assertEquals(24, animation.snapping());
        assertEquals(6, animation.animators().size());
    }

    @Test
    void evaluatesWheelRotationAtDifferentTimes() throws Exception {
        ModelAnimation animation = loadSpinAnimation();

        float[] times = { 0, 1, 2, 3, 4 };
        float[] expectedRotations = { 0, -90, -180, -270, -360 };

        for (int i = 0; i < times.length; i++) {
            Map<String, AnimationTransform> transforms =
                AnimationEvaluator.evaluate(animation, times[i]);

            AnimationTransform wheel = transforms.get(WHEEL_UUID);

            assertNotNull(wheel);
            assertEquals(
                expectedRotations[i],
                wheel.rotation().y(),
                0.001f,
                "Unexpected Wheel rotation at " + times[i] + " seconds."
            );
        }
    }

    @Test
    void evaluatesRollRotationAtDifferentTimes() throws Exception {
        ModelAnimation animation = loadSpinAnimation();

        float[] times = { 0, 1, 2, 3, 4 };
        float[] expectedRotations = { 0, -90, -180, -270, -360 };

        for (int i = 0; i < times.length; i++) {
            Map<String, AnimationTransform> transforms =
                AnimationEvaluator.evaluate(animation, times[i]);

            AnimationTransform roll = transforms.get(ROLL_UUID);

            assertNotNull(roll);
            assertEquals(
                expectedRotations[i],
                roll.rotation().x(),
                0.001f,
                "Unexpected Roll rotation at " + times[i] + " seconds."
            );
        }
    }

    @Test
    void preservesAllSixAnimationTargets() throws Exception {
        ModelAnimation animation = loadSpinAnimation();

        assertNotNull(animation.findAnimator(FRAME_UUID));
        assertNotNull(animation.findAnimator(WHEEL_UUID));
        assertNotNull(animation.findAnimator(ROLL_UUID));

        assertNotNull(
            animation.findAnimator("9dd45921-cb4d-c461-d19f-f7e009bbeaad")
        );

        assertNotNull(
            animation.findAnimator("3d022c02-6e78-5386-e964-80dadc7096e2")
        );

        assertNotNull(
            animation.findAnimator("7efe2ac9-d635-dd34-afca-6348cb86a9a4")
        );
    }

    @Test
    void actualAnimationLoopsCorrectly() throws Exception {
        ModelAnimation animation = loadSpinAnimation();

        AnimationController controller = new AnimationController();
        controller.play(animation);

        controller.update(4.5f);

        assertEquals(0.5f, controller.time(), 0.001f);
        assertEquals(AnimationPlaybackStatus.PLAYING, controller.status());

        Map<String, AnimationTransform> transforms =
            AnimationEvaluator.evaluate(animation, controller.time());

        assertEquals(-45.0f, transforms.get(WHEEL_UUID).rotation().y(), 0.001f);

        assertEquals(-45.0f, transforms.get(ROLL_UUID).rotation().x(), 0.001f);
    }

    @Test
    void actualAnimationCanBePausedAndResumed() throws Exception {
        ModelAnimation animation = loadSpinAnimation();

        AnimationController controller = new AnimationController();
        controller.play(animation);

        controller.update(1.0f);
        controller.pause();

        assertEquals(AnimationPlaybackStatus.PAUSED, controller.status());

        controller.update(1.0f);

        assertEquals(1.0f, controller.time(), 0.001f);

        controller.resume();
        controller.update(1.0f);

        assertEquals(2.0f, controller.time(), 0.001f);
        assertEquals(AnimationPlaybackStatus.PLAYING, controller.status());
    }

    @Test
    void buildsFrameAndWheelHierarchy() throws Exception {
        BlockbenchModel model = loadModel();

        ModelGeometryHierarchy hierarchy = ModelGeometryBuilder.buildHierarchy(
            model
        );

        ModelGeometryNode frame = findNode(hierarchy.roots(), FRAME_UUID);

        assertNotNull(frame, "Frame group was not found in the hierarchy.");

        ModelGeometryNode wheel = findNode(frame.children(), WHEEL_UUID);

        assertNotNull(wheel, "Wheel should be a child of Frame.");
    }

    private ModelGeometryNode findNode(
        Iterable<ModelGeometryNode> nodes,
        String uuid
    ) {
        for (ModelGeometryNode node : nodes) {
            if (uuid.equals(node.uuid())) {
                return node;
            }

            ModelGeometryNode child = findNode(node.children(), uuid);

            if (child != null) {
                return child;
            }
        }

        return null;
    }
}
