package com.hod.blockbench.validation;

import com.hod.blockbench.model.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BlockbenchModelValidator {

    private BlockbenchModelValidator() {}

    /**
     * Validates a parsed Blockbench model.
     *
     * @return validation issues, including errors and warnings
     */
    public static List<ValidationIssue> validate(BlockbenchModel model) {
        List<ValidationIssue> issues = new ArrayList<>();

        if (model == null) {
            addError(issues, "$", "Model is null.");
            return List.copyOf(issues);
        }

        validateMetadata(model, issues);
        validateElements(model, issues);
        validateTextures(model, issues);
        validateOutliner(model, issues);

        return List.copyOf(issues);
    }

    /**
     * Returns whether the model has no validation errors.
     * Warnings do not make a model invalid.
     */
    public static boolean isValid(BlockbenchModel model) {
        return validate(model)
            .stream()
            .noneMatch(issue -> issue.severity() == ValidationSeverity.ERROR);
    }

    /**
     * Returns only validation errors, excluding warnings.
     */
    public static List<ValidationIssue> getErrors(BlockbenchModel model) {
        return validate(model)
            .stream()
            .filter(issue -> issue.severity() == ValidationSeverity.ERROR)
            .toList();
    }

    /**
     * Returns only validation warnings.
     */
    public static List<ValidationIssue> getWarnings(BlockbenchModel model) {
        return validate(model)
            .stream()
            .filter(issue -> issue.severity() == ValidationSeverity.WARNING)
            .toList();
    }

    // ---------------------------------------------------------------------
    // Metadata
    // ---------------------------------------------------------------------

    private static void validateMetadata(
        BlockbenchModel model,
        List<ValidationIssue> issues
    ) {
        ModelMetadata metadata = model.metadata();

        if (metadata == null) {
            addError(issues, "meta", "Model metadata is missing.");
            return;
        }

        if (metadata.textureWidth() <= 0) {
            addError(
                issues,
                "meta.texture_width",
                "Texture width must be greater than zero."
            );
        }

        if (metadata.textureHeight() <= 0) {
            addError(
                issues,
                "meta.texture_height",
                "Texture height must be greater than zero."
            );
        }
    }

    // ---------------------------------------------------------------------
    // Elements and polygon geometry
    // ---------------------------------------------------------------------

    private static void validateElements(
        BlockbenchModel model,
        List<ValidationIssue> issues
    ) {
        Set<String> elementUuids = new HashSet<>();

        for (int i = 0; i < model.elements().size(); i++) {
            ModelElement element = model.elements().get(i);
            String path = "elements[" + i + "]";

            if (element == null) {
                addError(issues, path, "Element is null.");
                continue;
            }

            String uuid = element.uuid();

            if (uuid == null || uuid.isBlank()) {
                addError(issues, path + ".uuid", "Element UUID is missing.");
            } else if (!elementUuids.add(uuid)) {
                addError(
                    issues,
                    path + ".uuid",
                    "Duplicate element UUID: " + uuid
                );
            }

            if (element.origin() == null) {
                addError(
                    issues,
                    path + ".origin",
                    "Element origin is missing."
                );
            } else if (!isFinite(element.origin())) {
                addError(
                    issues,
                    path + ".origin",
                    "Element origin contains non-finite coordinates."
                );
            }

            if (element.rotation() == null) {
                addError(
                    issues,
                    path + ".rotation",
                    "Element rotation is missing."
                );
            } else if (!isFinite(element.rotation())) {
                addError(
                    issues,
                    path + ".rotation",
                    "Element rotation contains non-finite values."
                );
            }

            validateElementVertices(element, path, issues);
            validateElementFaces(model, element, path, issues);
        }
    }

    private static void validateElementVertices(
        ModelElement element,
        String elementPath,
        List<ValidationIssue> issues
    ) {
        Set<String> vertexUuids = new HashSet<>();

        for (var entry : element.vertices().entrySet()) {
            String uuid = entry.getKey();
            ModelVector3 position = entry.getValue();
            String path = elementPath + ".vertices." + uuid;

            if (uuid == null || uuid.isBlank()) {
                addError(
                    issues,
                    elementPath + ".vertices",
                    "Vertex UUID is missing."
                );
                continue;
            }

            if (!vertexUuids.add(uuid)) {
                addError(issues, path, "Duplicate vertex UUID: " + uuid);
            }

            if (position == null) {
                addError(issues, path, "Vertex position is missing.");
            } else if (!isFinite(position)) {
                addError(
                    issues,
                    path,
                    "Vertex contains non-finite coordinates."
                );
            }
        }
    }

    private static void validateElementFaces(
        BlockbenchModel model,
        ModelElement element,
        String elementPath,
        List<ValidationIssue> issues
    ) {
        Set<String> faceUuids = new HashSet<>();

        for (int i = 0; i < element.faces().size(); i++) {
            ModelPolygonFace face = element.faces().get(i);
            String path = elementPath + ".faces[" + i + "]";

            if (face == null) {
                addError(issues, path, "Face is null.");
                continue;
            }

            String faceUuid = face.uuid();

            if (faceUuid != null && !faceUuid.isBlank()) {
                if (!faceUuids.add(faceUuid)) {
                    addError(
                        issues,
                        path + ".uuid",
                        "Duplicate face UUID: " + faceUuid
                    );
                }
            }

            if (face.vertices() == null || face.vertices().size() < 3) {
                addError(
                    issues,
                    path + ".vertices",
                    "A polygon face must contain at least three vertices."
                );
                continue;
            }

            Set<String> referencedVertices = new HashSet<>();

            for (
                int cornerIndex = 0;
                cornerIndex < face.vertices().size();
                cornerIndex++
            ) {
                ModelFaceVertex corner = face.vertices().get(cornerIndex);
                String cornerPath = path + ".vertices[" + cornerIndex + "]";

                if (corner == null) {
                    addError(issues, cornerPath, "Face corner is null.");
                    continue;
                }

                String vertexUuid = corner.vertexUuid();

                if (vertexUuid == null || vertexUuid.isBlank()) {
                    addError(
                        issues,
                        cornerPath + ".vertexUuid",
                        "Face corner has no vertex UUID."
                    );
                } else {
                    if (!element.vertices().containsKey(vertexUuid)) {
                        addError(
                            issues,
                            cornerPath + ".vertexUuid",
                            "Face references missing vertex: " + vertexUuid
                        );
                    }

                    if (!referencedVertices.add(vertexUuid)) {
                        addWarning(
                            issues,
                            cornerPath + ".vertexUuid",
                            "Face references the same vertex more than once: " +
                                vertexUuid
                        );
                    }
                }

                if (
                    !Float.isFinite(corner.u()) || !Float.isFinite(corner.v())
                ) {
                    addError(
                        issues,
                        cornerPath + ".uv",
                        "Face corner has non-finite UV coordinates."
                    );
                }
            }

            validateFaceTexture(model, face, path, issues);
        }
    }

    private static void validateFaceTexture(
        BlockbenchModel model,
        ModelPolygonFace face,
        String path,
        List<ValidationIssue> issues
    ) {
        int texture = face.texture();

        if (texture < -1 || texture >= model.textures().size()) {
            addError(
                issues,
                path + ".texture",
                "Invalid texture index: " + texture
            );
        }

        if (texture == -1) {
            addWarning(
                issues,
                path + ".texture",
                "Face has no texture assigned."
            );
        }
    }

    // ---------------------------------------------------------------------
    // Textures
    // ---------------------------------------------------------------------

    private static void validateTextures(
        BlockbenchModel model,
        List<ValidationIssue> issues
    ) {
        Set<String> textureIds = new HashSet<>();

        for (int i = 0; i < model.textures().size(); i++) {
            ModelTexture texture = model.textures().get(i);
            String path = "textures[" + i + "]";

            if (texture == null) {
                addError(issues, path, "Texture is null.");
                continue;
            }

            String id = texture.id();

            if (id == null || id.isBlank()) {
                addError(issues, path + ".id", "Texture ID is missing.");
            } else if (!textureIds.add(id)) {
                addError(issues, path + ".id", "Duplicate texture ID: " + id);
            }

            if (texture.width() <= 0 || texture.height() <= 0) {
                addError(
                    issues,
                    path,
                    "Texture dimensions must be greater than zero."
                );
            }

            if (texture.uvWidth() <= 0 || texture.uvHeight() <= 0) {
                addError(
                    issues,
                    path,
                    "Texture UV dimensions must be greater than zero."
                );
            }

            validateTextureImage(texture, path, issues);
        }
    }

    private static void validateTextureImage(
        ModelTexture texture,
        String path,
        List<ValidationIssue> issues
    ) {
        if (!texture.hasEmbeddedImage()) {
            addWarning(
                issues,
                path + ".source",
                "Texture has no embedded image data."
            );
            return;
        }

        try {
            var image = texture.decodeImage();

            if (
                texture.width() > 0 &&
                texture.height() > 0 &&
                (image.getWidth() != texture.width() ||
                    image.getHeight() != texture.height())
            ) {
                addError(
                    issues,
                    path,
                    "Embedded image dimensions do not match " +
                        "the declared texture dimensions."
                );
            }
        } catch (IOException exception) {
            addError(
                issues,
                path + ".source",
                "Embedded texture image could not be decoded: " +
                    exception.getMessage()
            );
        }
    }

    // ---------------------------------------------------------------------
    // Outliner
    // ---------------------------------------------------------------------

    private static void validateOutliner(
        BlockbenchModel model,
        List<ValidationIssue> issues
    ) {
        Set<String> knownElementUuids = new HashSet<>();

        for (ModelElement element : model.elements()) {
            if (
                element != null &&
                element.uuid() != null &&
                !element.uuid().isBlank()
            ) {
                knownElementUuids.add(element.uuid());
            }
        }

        Set<String> referencedElementUuids = new HashSet<>();
        Set<String> nodeUuids = new HashSet<>();

        validateOutlinerNodes(
            model.outliner(),
            "outliner",
            knownElementUuids,
            referencedElementUuids,
            nodeUuids,
            issues
        );

        for (String uuid : knownElementUuids) {
            if (!referencedElementUuids.contains(uuid)) {
                addError(
                    issues,
                    "outliner",
                    "Element is missing from outliner: " + uuid
                );
            }
        }
    }

    private static void validateOutlinerNodes(
        List<ModelOutlinerNode> nodes,
        String parentPath,
        Set<String> knownElementUuids,
        Set<String> referencedElementUuids,
        Set<String> nodeUuids,
        List<ValidationIssue> issues
    ) {
        for (int i = 0; i < nodes.size(); i++) {
            ModelOutlinerNode node = nodes.get(i);
            String path = parentPath + "[" + i + "]";

            if (node == null) {
                addError(issues, path, "Outliner node is null.");
                continue;
            }

            String nodeUuid = node.uuid();

            if (nodeUuid != null && !nodeUuid.isBlank()) {
                if (!nodeUuids.add(nodeUuid)) {
                    addError(
                        issues,
                        path + ".uuid",
                        "Duplicate outliner node UUID: " + nodeUuid
                    );
                }
            } else {
                addWarning(
                    issues,
                    path + ".uuid",
                    "Outliner node has no UUID."
                );
            }

            if (node.type() == null) {
                addError(issues, path + ".type", "Outliner node has no type.");
            } else if (node.type() == ModelOutlinerNode.Type.ELEMENT) {
                String uuid = node.elementUuid();

                if (uuid == null || uuid.isBlank()) {
                    addError(
                        issues,
                        path + ".elementUuid",
                        "Outliner element node has no element UUID."
                    );
                } else if (!knownElementUuids.contains(uuid)) {
                    addError(
                        issues,
                        path + ".elementUuid",
                        "Outliner references unknown element: " + uuid
                    );
                } else if (!referencedElementUuids.add(uuid)) {
                    addError(
                        issues,
                        path + ".elementUuid",
                        "Element appears more than once in outliner: " + uuid
                    );
                }
            }

            if (node.origin() != null && !isFinite(node.origin())) {
                addError(
                    issues,
                    path + ".origin",
                    "Outliner node origin contains non-finite coordinates."
                );
            }

            if (node.rotation() != null && !isFinite(node.rotation())) {
                addError(
                    issues,
                    path + ".rotation",
                    "Outliner node rotation contains non-finite values."
                );
            }

            validateOutlinerNodes(
                node.children(),
                path + ".children",
                knownElementUuids,
                referencedElementUuids,
                nodeUuids,
                issues
            );
        }
    }

    // ---------------------------------------------------------------------
    // Numeric helpers
    // ---------------------------------------------------------------------

    private static boolean isFinite(ModelVector3 vector) {
        return (
            Float.isFinite(vector.x()) &&
            Float.isFinite(vector.y()) &&
            Float.isFinite(vector.z())
        );
    }

    // ---------------------------------------------------------------------
    // Issue helpers
    // ---------------------------------------------------------------------

    private static void addError(
        List<ValidationIssue> issues,
        String path,
        String message
    ) {
        issues.add(
            new ValidationIssue(ValidationSeverity.ERROR, path, message)
        );
    }

    private static void addWarning(
        List<ValidationIssue> issues,
        String path,
        String message
    ) {
        issues.add(
            new ValidationIssue(ValidationSeverity.WARNING, path, message)
        );
    }
}
