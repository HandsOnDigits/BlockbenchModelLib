package com.hod.blockbench.common.geometry;

import com.hod.blockbench.model.BlockbenchModel;
import com.hod.blockbench.model.ModelCube;
import com.hod.blockbench.model.ModelFace;
import com.hod.blockbench.model.ModelOutlinerNode;
import com.hod.blockbench.model.ModelTexture;
import com.hod.blockbench.model.ModelUvTransform;
import com.hod.blockbench.model.ModelVector3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class ModelGeometryBuilder {

    private ModelGeometryBuilder() {
    }

    private static int[] uvIndices(ModelFace.Direction direction) {
        return switch (direction) {
            case NORTH -> new int[]{1, 0, 3, 2};
            case SOUTH -> new int[]{1, 0, 3, 2};
            case EAST  -> new int[]{0, 1, 2, 3};
            case WEST  -> new int[]{1, 0, 3, 2};
            case UP    -> new int[]{0, 3, 2, 1};
            case DOWN  -> new int[]{0, 3, 2, 1};
        };
    }

    public static ModelGeometry build(BlockbenchModel model) {
        List<ModelQuad> quads = new ArrayList<>();

        Map<String, ModelCube> cubesByUuid = new HashMap<>();

        for (ModelCube cube : model.cubes()) {
            cubesByUuid.put(cube.uuid(), cube);
        }

        Set<String> referencedCubes = new HashSet<>();
        collectCubeReferences(model.outliner(), referencedCubes);

        Set<String> emittedCubes = new HashSet<>();

        for (ModelOutlinerNode node : model.outliner()) {
            buildNode(
                    model,
                    node,
                    cubesByUuid,
                    ModelTransform.identity(),
                    true,
                    emittedCubes,
                    quads
            );
        }

        // Include cubes that do not appear in the outliner.
        for (ModelCube cube : model.cubes()) {
            if (referencedCubes.contains(cube.uuid())) {
                continue;
            }

            if (!emittedCubes.add(cube.uuid())) {
                continue;
            }

            buildCube(
                    model,
                    cube,
                    ModelTransform.identity(),
                    quads
            );
        }

        return new ModelGeometry(quads);
    }

    private static void collectCubeReferences(
            List<ModelOutlinerNode> nodes,
            Set<String> references
    ) {
        for (ModelOutlinerNode node : nodes) {
            if (node.isCube() && node.cubeUuid() != null) {
                references.add(node.cubeUuid());
            }

            collectCubeReferences(node.children(), references);
        }
    }

    private static void buildNode(
            BlockbenchModel model,
            ModelOutlinerNode node,
            Map<String, ModelCube> cubesByUuid,
            ModelTransform parentTransform,
            boolean parentVisible,
            Set<String> emittedCubes,
            List<ModelQuad> output
    ) {
        boolean visible = parentVisible && node.visible();

        if (!visible) {
            return;
        }

        ModelTransform localTransform = ModelTransform.identity();

        if (node.isGroup()) {
            localTransform = ModelTransform.aroundOrigin(
                    originOrZero(node.origin()),
                    rotationOrZero(node.rotation())
            );
        }

        ModelTransform accumulatedTransform =
                parentTransform.multiply(localTransform);

        if (node.isCube()) {
            String cubeUuid = node.cubeUuid();

            if (cubeUuid == null || !emittedCubes.add(cubeUuid)) {
                return;
            }

            ModelCube cube = cubesByUuid.get(cubeUuid);

            if (cube == null) {
                return;
            }

            buildCube(
                    model,
                    cube,
                    accumulatedTransform,
                    output
            );

            return;
        }

        for (ModelOutlinerNode child : node.children()) {
            buildNode(
                    model,
                    child,
                    cubesByUuid,
                    accumulatedTransform,
                    visible,
                    emittedCubes,
                    output
            );
        }
    }

    private static void buildCube(
            BlockbenchModel model,
            ModelCube cube,
            ModelTransform parentTransform,
            List<ModelQuad> output
    ) {
        if (!cube.visible()) {
            return;
        }

        ModelVector3 inflation = new ModelVector3(
                cube.inflate(),
                cube.inflate(),
                cube.inflate()
        );

        ModelVector3 from = cube.from().subtract(inflation);
        ModelVector3 to = cube.to().add(inflation);

        ModelTransform cubeTransform = ModelTransform.aroundOrigin(
                originOrZero(cube.origin()),
                rotationOrZero(cube.rotation())
        );

        ModelTransform transform = parentTransform.multiply(cubeTransform);

        for (ModelFace.Direction direction : ModelFace.Direction.values()) {
            ModelFace face = cube.faces().get(direction);

            if (face == null) {
                continue;
            }

            ModelTexture texture = cube.findTexture(direction, model);

            if (texture == null) {
                continue;
            }

            output.add(buildFace(
                    direction,
                    face,
                    texture,
                    from,
                    to,
                    transform
            ));
        }
    }

    private static ModelQuad buildFace(
            ModelFace.Direction direction,
            ModelFace face,
            ModelTexture texture,
            ModelVector3 from,
            ModelVector3 to,
            ModelTransform transform
    ) {
        List<ModelUvTransform.UvPoint> uv =
                ModelUvTransform.getCorners(face, texture);

        List<ModelVector3> positions = positions(
                direction,
                from,
                to
        );

        ModelVector3 normal = transform.transformDirection(
                normal(direction)
        );

        int[] uvIndices = uvIndices(direction);

        List<ModelVertex> vertices = new ArrayList<>(4);

        for (int i = 0; i < 4; i++) {
            ModelVector3 position = transform.transformPosition(
                    positions.get(i)
            );

            ModelUvTransform.UvPoint uvPoint = uv.get(uvIndices[i]);

            vertices.add(new ModelVertex(
                    position,
                    uvPoint.u(),
                    uvPoint.v(),
                    normal
            ));
        }

        return new ModelQuad(
                direction,
                face.texture(),
                vertices
        );
    }

    private static ModelVector3 originOrZero(ModelVector3 origin) {
        return origin != null ? origin : ModelVector3.ZERO;
    }

    private static ModelVector3 rotationOrZero(ModelVector3 rotation) {
        return rotation != null ? rotation : ModelVector3.ZERO;
    }

    private static List<ModelVector3> positions(
            ModelFace.Direction direction,
            ModelVector3 from,
            ModelVector3 to
    ) {
        float x1 = from.x();
        float y1 = from.y();
        float z1 = from.z();

        float x2 = to.x();
        float y2 = to.y();
        float z2 = to.z();

        return switch (direction) {
            case NORTH -> List.of(
                    new ModelVector3(x1, y2, z1),
                    new ModelVector3(x2, y2, z1),
                    new ModelVector3(x2, y1, z1),
                    new ModelVector3(x1, y1, z1)
            );

            case SOUTH -> List.of(
                    new ModelVector3(x2, y2, z2),
                    new ModelVector3(x1, y2, z2),
                    new ModelVector3(x1, y1, z2),
                    new ModelVector3(x2, y1, z2)
            );

            case EAST -> List.of(
                    new ModelVector3(x2, y2, z1),
                    new ModelVector3(x2, y2, z2),
                    new ModelVector3(x2, y1, z2),
                    new ModelVector3(x2, y1, z1)
            );

            case WEST -> List.of(
                    new ModelVector3(x1, y2, z2),
                    new ModelVector3(x1, y2, z1),
                    new ModelVector3(x1, y1, z1),
                    new ModelVector3(x1, y1, z2)
            );

            case UP -> List.of(
                    new ModelVector3(x1, y2, z1),
                    new ModelVector3(x1, y2, z2),
                    new ModelVector3(x2, y2, z2),
                    new ModelVector3(x2, y2, z1)
            );

            case DOWN -> List.of(
                    new ModelVector3(x1, y1, z2),
                    new ModelVector3(x1, y1, z1),
                    new ModelVector3(x2, y1, z1),
                    new ModelVector3(x2, y1, z2)
            );
        };
    }

    private static ModelVector3 normal(
            ModelFace.Direction direction
    ) {
        return switch (direction) {
            case NORTH -> new ModelVector3(0, 0, -1);
            case SOUTH -> new ModelVector3(0, 0, 1);
            case EAST -> new ModelVector3(1, 0, 0);
            case WEST -> new ModelVector3(-1, 0, 0);
            case UP -> new ModelVector3(0, 1, 0);
            case DOWN -> new ModelVector3(0, -1, 0);
        };
    }
}