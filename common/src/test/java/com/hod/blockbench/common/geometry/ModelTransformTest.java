package com.hod.blockbench.common.geometry;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.hod.blockbench.model.ModelVector3;
import org.junit.jupiter.api.Test;

class ModelTransformTest {

    private static final float EPSILON = 0.0001f;

    @Test
    void rotationXRotatesAroundXAxis() {
        ModelVector3 result = ModelTransform.rotationX(90.0f).transformPosition(
            new ModelVector3(0, 1, 0)
        );

        assertVectorEquals(new ModelVector3(0, 0, 1), result);
    }

    @Test
    void rotationYRotatesAroundYAxis() {
        ModelVector3 result = ModelTransform.rotationY(90.0f).transformPosition(
            new ModelVector3(1, 0, 0)
        );

        assertVectorEquals(new ModelVector3(0, 0, -1), result);
    }

    @Test
    void rotationZRotatesAroundZAxis() {
        ModelVector3 result = ModelTransform.rotationZ(90.0f).transformPosition(
            new ModelVector3(1, 0, 0)
        );

        assertVectorEquals(new ModelVector3(0, 1, 0), result);
    }

    @Test
    void rotationAroundOriginKeepsPivotStationary() {
        ModelVector3 pivot = new ModelVector3(4, 0, 0);

        ModelTransform transform = ModelTransform.aroundOrigin(
            pivot,
            new ModelVector3(0, 0, 90)
        );

        assertVectorEquals(pivot, transform.transformPosition(pivot));
        assertVectorEquals(
            new ModelVector3(4, 1, 0),
            transform.transformPosition(new ModelVector3(5, 0, 0))
        );
    }

    @Test
    void nestedTransformsApplyChildBeforeParent() {
        ModelTransform parent = ModelTransform.aroundOrigin(
            ModelVector3.ZERO,
            new ModelVector3(0, 0, 90)
        );

        ModelTransform child = ModelTransform.aroundOrigin(
            new ModelVector3(1, 0, 0),
            new ModelVector3(0, 0, 90)
        );

        ModelVector3 result = parent
            .multiply(child)
            .transformPosition(new ModelVector3(2, 0, 0));

        assertVectorEquals(new ModelVector3(-1, 1, 0), result);
    }

    @Test
    void eulerRotationAppliesXThenYThenZ() {
        ModelVector3 result = ModelTransform.rotation(
            new ModelVector3(90, 90, 90)
        ).transformPosition(new ModelVector3(1, 2, 3));

        assertVectorEquals(new ModelVector3(3, 2, -1), result);
    }

    private static void assertVectorEquals(
        ModelVector3 expected,
        ModelVector3 actual
    ) {
        assertEquals(expected.x(), actual.x(), EPSILON, "X component");
        assertEquals(expected.y(), actual.y(), EPSILON, "Y component");
        assertEquals(expected.z(), actual.z(), EPSILON, "Z component");
    }
}
