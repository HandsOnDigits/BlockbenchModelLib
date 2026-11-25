package com.hod.blockbench.common.geometry;

import com.hod.blockbench.model.ModelFace;
import com.hod.blockbench.model.ModelTexture;
import com.hod.blockbench.model.ModelUvTransform;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelUvTransformTest {

    private static final ModelTexture TEXTURE = new ModelTexture(
            "texture-1",
            "test",
            16,
            16,
            16,
            16,
            1,
            "custom",
            "",
            false,
            "",
            null
    );

    private static final ModelFace.ModelUv UV =
            new ModelFace.ModelUv(2, 4, 10, 12);

    @Test
    void zeroRotationPreservesUvCorners() {
        assertCorners(0, List.of(
                point(0.125f, 0.25f),
                point(0.625f, 0.25f),
                point(0.625f, 0.75f),
                point(0.125f, 0.75f)
        ));
    }

    @Test
    void ninetyDegreeRotationRotatesCornersClockwise() {
        assertCorners(90, List.of(
                point(0.625f, 0.25f),
                point(0.625f, 0.75f),
                point(0.125f, 0.75f),
                point(0.125f, 0.25f)
        ));
    }

    @Test
    void oneHundredEightyDegreeRotationRotatesCorners() {
        assertCorners(180, List.of(
                point(0.625f, 0.75f),
                point(0.125f, 0.75f),
                point(0.125f, 0.25f),
                point(0.625f, 0.25f)
        ));
    }

    @Test
    void twoHundredSeventyDegreeRotationRotatesCorners() {
        assertCorners(270, List.of(
                point(0.125f, 0.75f),
                point(0.125f, 0.25f),
                point(0.625f, 0.25f),
                point(0.625f, 0.75f)
        ));
    }

    private static void assertCorners(
            int rotation,
            List<ModelUvTransform.UvPoint> expected
    ) {
        ModelFace face = new ModelFace(
                ModelFace.Direction.NORTH,
                UV,
                0,
                rotation
        );

        List<ModelUvTransform.UvPoint> actual =
                ModelUvTransform.getCorners(face, TEXTURE);

        assertEquals(4, actual.size());

        for (int i = 0; i < 4; i++) {
            assertEquals(expected.get(i).u(), actual.get(i).u(), 0.0001f);
            assertEquals(expected.get(i).v(), actual.get(i).v(), 0.0001f);
        }
    }

    private static ModelUvTransform.UvPoint point(float u, float v) {
        return new ModelUvTransform.UvPoint(u, v);
    }
}