package com.hod.blockbench.model;

import java.util.ArrayList;
import java.util.List;

public record BlockbenchModel(
        String name,
        String modelIdentifier,
        ModelMetadata metadata,
        List<ModelCube> cubes,
        List<ModelOutlinerNode> outliner,
        List<ModelTexture> textures,
        String sourceJson
) {
    public BlockbenchModel {
        cubes = List.copyOf(cubes);
        outliner = List.copyOf(outliner);
        textures = List.copyOf(textures);
    }

    /**
     * Finds a cube by its UUID.
     *
     * @return the cube, or null if it does not exist
     */
    public ModelCube findCube(String uuid) {
        if (uuid == null) {
            return null;
        }

        return cubes.stream()
                .filter(cube -> cube.uuid().equals(uuid))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds a texture by its ID.
     *
     * @return the texture, or null if it does not exist
     */
    public ModelTexture findTexture(String id) {
        if (id == null) {
            return null;
        }

        return textures.stream()
                .filter(texture -> texture.id().equals(id))
                .findFirst()
                .orElse(null);
    }

    /**
     * Finds the first outliner node with the specified UUID.
     *
     * @return the node, or null if it does not exist
     */
    public ModelOutlinerNode findNodeByUuid(String uuid) {
        if (uuid == null) {
            return null;
        }

        return findNodeByUuid(outliner, uuid);
    }

    private ModelOutlinerNode findNodeByUuid(
            List<ModelOutlinerNode> nodes,
            String uuid
    ) {
        for (ModelOutlinerNode node : nodes) {
            if (uuid.equals(node.uuid())) {
                return node;
            }

            ModelOutlinerNode result =
                    findNodeByUuid(node.children(), uuid);

            if (result != null) {
                return result;
            }
        }

        return null;
    }

    /**
     * Finds every outliner node with the specified name.
     * Names are not guaranteed to be unique.
     */
    public List<ModelOutlinerNode> findNodesByName(String name) {
        if (name == null) {
            return List.of();
        }

        List<ModelOutlinerNode> result = new ArrayList<>();
        collectNodesByName(outliner, name, result);

        return List.copyOf(result);
    }

    private void collectNodesByName(
            List<ModelOutlinerNode> nodes,
            String name,
            List<ModelOutlinerNode> result
    ) {
        for (ModelOutlinerNode node : nodes) {
            if (name.equals(node.name())) {
                result.add(node);
            }

            collectNodesByName(node.children(), name, result);
        }
    }

    /**
     * Finds all cubes referenced by a node and its descendants.
     *
     * @return an immutable list of cubes
     */
    public List<ModelCube> findCubesInNode(ModelOutlinerNode node) {
        if (node == null) {
            return List.of();
        }

        List<ModelCube> result = new ArrayList<>();
        collectCubes(node, result);

        return List.copyOf(result);
    }

    private void collectCubes(
            ModelOutlinerNode node,
            List<ModelCube> result
    ) {
        if (node.type() == ModelOutlinerNode.Type.CUBE) {
            ModelCube cube = findCube(node.cubeUuid());

            if (cube != null) {
                result.add(cube);
            }
        }

        for (ModelOutlinerNode child : node.children()) {
            collectCubes(child, result);
        }
    }

    /**
     * Finds all cubes referenced anywhere in the outliner.
     *
     * @return an immutable list of cubes
     */
    public List<ModelCube> findAllOutlinerCubes() {
        List<ModelCube> result = new ArrayList<>();

        for (ModelOutlinerNode node : outliner) {
            collectCubes(node, result);
        }

        return List.copyOf(result);
    }
}