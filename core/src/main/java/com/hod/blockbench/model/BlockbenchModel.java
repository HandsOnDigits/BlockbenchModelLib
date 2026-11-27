package com.hod.blockbench.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record BlockbenchModel(
    String name,
    String modelIdentifier,
    ModelMetadata metadata,
    List<ModelElement> elements,
    List<ModelOutlinerNode> outliner,
    List<ModelTexture> textures,
    List<ModelAnimation> animations,
    String sourceJson
) {
    public BlockbenchModel {
        elements = List.copyOf(elements);
        outliner = List.copyOf(outliner);
        textures = List.copyOf(textures);
        animations = List.copyOf(animations);
        sourceJson = sourceJson != null ? sourceJson : "";
    }

    public Optional<ModelElement> findElement(String uuid) {
        return elements
            .stream()
            .filter(element -> element.uuid().equals(uuid))
            .findFirst();
    }

    public Optional<ModelTexture> findTexture(int index) {
        if (index < 0 || index >= textures.size()) {
            return Optional.empty();
        }

        return Optional.of(textures.get(index));
    }

    public Optional<ModelAnimation> findAnimation(String uuid) {
        return animations
            .stream()
            .filter(animation -> animation.uuid().equals(uuid))
            .findFirst();
    }

    public List<ModelAnimation> findAnimationsByName(String name) {
        return animations
            .stream()
            .filter(animation -> animation.name().equals(name))
            .toList();
    }

    public Optional<ModelOutlinerNode> findNodeByUuid(String uuid) {
        return findNodeByUuid(outliner, uuid);
    }

    private static Optional<ModelOutlinerNode> findNodeByUuid(
        List<ModelOutlinerNode> nodes,
        String uuid
    ) {
        for (ModelOutlinerNode node : nodes) {
            if (uuid.equals(node.uuid())) {
                return Optional.of(node);
            }

            Optional<ModelOutlinerNode> child = findNodeByUuid(
                node.children(),
                uuid
            );

            if (child.isPresent()) {
                return child;
            }
        }

        return Optional.empty();
    }

    public List<ModelOutlinerNode> findNodesByName(String name) {
        List<ModelOutlinerNode> result = new ArrayList<>();
        collectNodesByName(outliner, name, result);
        return List.copyOf(result);
    }

    private static void collectNodesByName(
        List<ModelOutlinerNode> nodes,
        String name,
        List<ModelOutlinerNode> result
    ) {
        for (ModelOutlinerNode node : nodes) {
            if (node.name().equals(name)) {
                result.add(node);
            }

            collectNodesByName(node.children(), name, result);
        }
    }

    public List<ModelElement> findElementsInNode(ModelOutlinerNode node) {
        List<ModelElement> result = new ArrayList<>();
        collectElements(node, result);
        return List.copyOf(result);
    }

    public List<ModelElement> findAllOutlinerElements() {
        List<ModelElement> result = new ArrayList<>();

        for (ModelOutlinerNode node : outliner) {
            collectElements(node, result);
        }

        return List.copyOf(result);
    }

    private void collectElements(
        ModelOutlinerNode node,
        List<ModelElement> result
    ) {
        if (node.isElement() && node.elementUuid() != null) {
            findElement(node.elementUuid()).ifPresent(result::add);
        }

        for (ModelOutlinerNode child : node.children()) {
            collectElements(child, result);
        }
    }
}
