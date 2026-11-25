package com.hod.blockbench.common.geometry;

import com.hod.blockbench.model.ModelVector3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ModelTransformTest {

    private static final float EPSILON = 0.00001f;

    @Test
    void identityPreservesPosition() {
        ModelVector3 position = new ModelVector3(2, 4, 6);

        ModelVector3 result = ModelTransform.identity()
                .transformPosition(position);

        assertVectorEquals(position, result);
    }

    @Test
    void translationMovesPosition() {
        ModelTransform transform = ModelTransform.translation(
                10, 20, 30
        );

        ModelVector3 result = transform.transformPosition(
                new ModelVector3(1, 2, 3)
        );

        assertVectorEquals(
                new ModelVector3(11, 22, 33),
                result
        );
    }

    @Test
    void rotationAroundZRotatesPosition() {
        ModelTransform transform = ModelTransform.rotationZ(90);

        ModelVector3 result = transform.transformPosition(
                new ModelVector3(1, 0, 0)
        );

        assertVectorEquals(
                new ModelVector3(0, 1, 0),
                result
        );
    }

    @Test
    void rotationAroundOriginPreservesOrigin() {
        ModelVector3 origin = new ModelVector3(4, 2, 0);

        ModelTransform transform = ModelTransform.aroundOrigin(
                origin,
                new ModelVector3(0, 0, 90)
        );

        ModelVector3 result = transform.transformPosition(origin);

        assertVectorEquals(origin, result);
    }

    @Test
    void rotationAroundOriginRotatesRelativePosition() {
        ModelTransform transform = ModelTransform.aroundOrigin(
                new ModelVector3(1, 0, 0),
                new ModelVector3(0, 0, 90)
        );

        ModelVector3 result = transform.transformPosition(
                new ModelVector3(2, 0, 0)
        );

        assertVectorEquals(
                new ModelVector3(1, 1, 0),
                result
        );
    }

    @Test
    void transformationsComposeParentBeforeChild() {
        ModelTransform parent = ModelTransform.translation(
                10, 0, 0
        );

        ModelTransform child = ModelTransform.rotationZ(90);

        ModelVector3 result = parent
                .multiply(child)
                .transformPosition(new ModelVector3(1, 0, 0));

        assertVectorEquals(
                new ModelVector3(10, 1, 0),
                result
        );
    }

    @Test
    void directionIgnoresTranslation() {
        ModelTransform transform = ModelTransform.translation(
                10, 20, 30
        );

        ModelVector3 result = transform.transformDirection(
                new ModelVector3(1, 0, 0)
        );

        assertVectorEquals(
                new ModelVector3(1, 0, 0),
                result
        );
    }

    private static void assertVectorEquals(
            ModelVector3 expected,
            ModelVector3 actual
    ) {
        assertEquals(expected.x(), actual.x(), EPSILON);
        assertEquals(expected.y(), actual.y(), EPSILON);
        assertEquals(expected.z(), actual.z(), EPSILON);
    }
}