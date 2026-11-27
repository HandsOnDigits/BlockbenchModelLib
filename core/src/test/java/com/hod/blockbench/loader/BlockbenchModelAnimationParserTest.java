package com.hod.blockbench.loader;

import static org.junit.jupiter.api.Assertions.*;

import com.hod.blockbench.model.AnimationKeyframe;
import com.hod.blockbench.model.BlockbenchModel;
import com.hod.blockbench.model.ModelAnimation;
import com.hod.blockbench.model.ModelAnimator;
import java.io.StringReader;
import java.util.List;
import org.junit.jupiter.api.Test;

class BlockbenchModelAnimationParserTest {

    private static final String MODEL_JSON = """
    {
      "name": "Spinning Wheel",
      "model_identifier": "spinning_wheel",
      "meta": {
        "format_version": "4.10",
        "model_format": "free",
        "box_uv": false
      },
      "resolution": {
        "width": 16,
        "height": 16
      },
      "elements": [],
      "textures": [],
      "outliner": [],
      "groups": [],
      "animations": [
        {
          "uuid": "1ee14373-0d60-6e88-9742-b8a82be67dd1",
          "name": "Spin",
          "loop": "loop",
          "override": false,
          "length": 4,
          "snapping": 24,
          "selected": true,
          "group_name": "",
          "scope": 0,
          "anim_time_update": "",
          "blend_weight": "",
          "start_delay": "",
          "loop_delay": "",
          "animators": {
            "3e2656fc-fb21-7437-63c6-8f9a19f45ce0": {
              "name": "Frame",
              "type": "bone",
              "rotation_global": false,
              "quaternion_interpolation": false
            },
            "e5e6cef6-a49f-7ebc-0302-1d635d6b6d41": {
              "name": "Wheel",
              "type": "bone",
              "rotation_global": false,
              "quaternion_interpolation": false,
              "keyframes": [
                {
                  "channel": "rotation",
                  "data_points": [
                    { "x": "0", "y": "0", "z": "0" }
                  ],
                  "uuid": "4fed3052-85c8-ff92-8c67-4b01d95567cd",
                  "time": 0,
                  "color": -1,
                  "interpolation": "linear"
                },
                {
                  "channel": "rotation",
                  "data_points": [
                    { "x": "0", "y": "-180\\n\\n", "z": "0" }
                  ],
                  "uuid": "81c453bb-7576-6511-1f6d-71b297946a22",
                  "time": 2,
                  "color": -1,
                  "interpolation": "linear"
                },
                {
                  "channel": "rotation",
                  "data_points": [
                    { "x": "0", "y": "-360\\n", "z": "0" }
                  ],
                  "uuid": "e890697e-1868-49ce-c5ff-860d0351dac0",
                  "time": 4,
                  "color": -1,
                  "interpolation": "linear"
                }
              ]
            },
            "0226d058-7e2d-639f-067f-4e6b961ae6a7": {
              "name": "Roll",
              "type": "bone",
              "rotation_global": false,
              "quaternion_interpolation": false,
              "keyframes": [
                {
                  "channel": "rotation",
                  "data_points": [
                    { "x": "0", "y": "0", "z": "0" }
                  ],
                  "uuid": "342c03f0-b752-6cd0-c983-7060ab0e0c0f",
                  "time": 0,
                  "color": -1,
                  "interpolation": "linear"
                },
                {
                  "channel": "rotation",
                  "data_points": [
                    { "x": "-180\\n\\n", "y": "0", "z": "0" }
                  ],
                  "uuid": "93b9febe-a670-04ff-64bd-fe82844ed6d1",
                  "time": 2,
                  "color": -1,
                  "interpolation": "linear"
                },
                {
                  "channel": "rotation",
                  "data_points": [
                    { "x": "-360\\n", "y": "0", "z": "0" }
                  ],
                  "uuid": "efdc2759-eca9-d40b-8b3c-541a189ea281",
                  "time": 4,
                  "color": -1,
                  "interpolation": "linear"
                }
              ]
            },
            "9dd45921-cb4d-c461-d19f-f7e009bbeaad": {
              "name": "Stepper",
              "type": "bone",
              "rotation_global": false,
              "quaternion_interpolation": false
            },
            "3d022c02-6e78-5386-e964-80dadc7096e2": {
              "name": "pen",
              "type": "bone",
              "rotation_global": false,
              "quaternion_interpolation": false,
              "keyframes": [
                {
                  "channel": "position",
                  "data_points": [
                    { "x": "0", "y": "0", "z": "0" }
                  ],
                  "uuid": "ca4aa253-7c9e-afc7-4ce0-c7243b6bd6bf",
                  "time": 0,
                  "color": -1,
                  "interpolation": "linear"
                }
              ]
            },
            "7efe2ac9-d635-dd34-afca-6348cb86a9a4": {
              "name": "foot",
              "type": "bone",
              "rotation_global": false,
              "quaternion_interpolation": false
            }
          }
        }
      ]
    }
    """;

    @Test
    void parsesSpinningWheelAnimation() throws Exception {
        BlockbenchModel model = BlockbenchModelLoader.load(
            new StringReader(MODEL_JSON)
        );

        assertEquals("Spinning Wheel", model.name());
        assertEquals(1, model.animations().size());

        ModelAnimation animation = model.findAnimation(
            "1ee14373-0d60-6e88-9742-b8a82be67dd1"
        );

        assertNotNull(animation);
        assertEquals("Spin", animation.name());
        assertEquals("loop", animation.loop());
        assertTrue(animation.isLooping());
        assertFalse(animation.override());
        assertEquals(4.0f, animation.length());
        assertEquals(24, animation.snapping());
        assertTrue(animation.selected());

        assertEquals(6, animation.animators().size());
    }

    @Test
    void preservesAnimatorsWithoutKeyframes() throws Exception {
        BlockbenchModel model = loadModel();
        ModelAnimation animation = model.findAnimationsByName("Spin").get(0);

        ModelAnimator frame = animation.findAnimator(
            "3e2656fc-fb21-7437-63c6-8f9a19f45ce0"
        );

        ModelAnimator stepper = animation.findAnimator(
            "9dd45921-cb4d-c461-d19f-f7e009bbeaad"
        );

        ModelAnimator foot = animation.findAnimator(
            "7efe2ac9-d635-dd34-afca-6348cb86a9a4"
        );

        assertNotNull(frame);
        assertNotNull(stepper);
        assertNotNull(foot);

        assertEquals("Frame", frame.name());
        assertEquals("Stepper", stepper.name());
        assertEquals("foot", foot.name());

        assertTrue(frame.keyframes().isEmpty());
        assertTrue(stepper.keyframes().isEmpty());
        assertTrue(foot.keyframes().isEmpty());
    }

    @Test
    void parsesWheelRotationKeyframes() throws Exception {
        ModelAnimation animation = loadAnimation();

        ModelAnimator wheel = animation.findAnimator(
            "e5e6cef6-a49f-7ebc-0302-1d635d6b6d41"
        );

        assertNotNull(wheel);
        assertEquals("Wheel", wheel.name());
        assertEquals("bone", wheel.type());
        assertEquals(3, wheel.keyframes().size());

        assertKeyframe(wheel.keyframes().get(0), 0.0f, "0");
        assertKeyframe(wheel.keyframes().get(1), 2.0f, "-180\n\n");
        assertKeyframe(wheel.keyframes().get(2), 4.0f, "-360\n");
    }

    @Test
    void preservesNewlinesButProvidesNumericValues() throws Exception {
        ModelAnimation animation = loadAnimation();

        ModelAnimator wheel = animation.findAnimator(
            "e5e6cef6-a49f-7ebc-0302-1d635d6b6d41"
        );

        var midpoint = wheel.keyframes().get(1).firstDataPoint();

        assertEquals("-180\n\n", midpoint.y());
        assertEquals(-180.0f, midpoint.yValue());
    }

    @Test
    void parsesRollAndPenAnimations() throws Exception {
        ModelAnimation animation = loadAnimation();

        ModelAnimator roll = animation.findAnimator(
            "0226d058-7e2d-639f-067f-4e6b961ae6a7"
        );

        ModelAnimator pen = animation.findAnimator(
            "3d022c02-6e78-5386-e964-80dadc7096e2"
        );

        assertNotNull(roll);
        assertNotNull(pen);

        assertEquals(3, roll.keyframes().size());
        assertEquals(1, pen.keyframes().size());

        assertEquals("rotation", roll.keyframes().get(0).channel());

        assertEquals("position", pen.keyframes().get(0).channel());

        assertEquals(
            -180.0f,
            roll.keyframes().get(1).firstDataPoint().xValue()
        );
    }

    private static void assertKeyframe(
        AnimationKeyframe keyframe,
        float expectedTime,
        String expectedY
    ) {
        assertEquals("rotation", keyframe.channel());
        assertEquals("linear", keyframe.interpolation());
        assertEquals(expectedTime, keyframe.time());

        assertNotNull(keyframe.firstDataPoint());
        assertEquals(expectedY, keyframe.firstDataPoint().y());
    }

    private static BlockbenchModel loadModel() throws Exception {
        return BlockbenchModelLoader.load(new StringReader(MODEL_JSON));
    }

    private static ModelAnimation loadAnimation() throws Exception {
        return loadModel().findAnimationsByName("Spin").get(0);
    }
}
