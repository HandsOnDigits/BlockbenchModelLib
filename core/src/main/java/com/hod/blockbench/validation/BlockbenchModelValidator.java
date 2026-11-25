package com.hod.blockbench.validation;

import com.hod.blockbench.model.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BlockbenchModelValidator {

    private BlockbenchModelValidator() {
    }

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
        validateCubes(model, issues);
        validateTextures(model, issues);
        validateOutliner(model, issues);

        return List.copyOf(issues);
    }

    /**
     * Returns whether the model has no validation errors.
     * Warnings do not make a model invalid.
     */
    public static boolean isValid(BlockbenchModel model) {
        return validate(model).stream()
                .noneMatch(issue ->
                        issue.severity() == ValidationSeverity.ERROR
                );
    }

    /**
     * Returns only validation errors, excluding warnings.
     */
    public static List<ValidationIssue> getErrors(BlockbenchModel model) {
        return validate(model).stream()
                .filter(issue ->
                        issue.severity() == ValidationSeverity.ERROR
                )
                .toList();
    }

    /**
     * Returns only validation warnings.
     */
    public static List<ValidationIssue> getWarnings(BlockbenchModel model) {
        return validate(model).stream()
                .filter(issue ->
                        issue.severity() == ValidationSeverity.WARNING
                )
                .toList();
    }

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

    private static void validateCubes(
            BlockbenchModel model,
            List<ValidationIssue> issues
    ) {
        Set<String> cubeUuids = new HashSet<>();

        for (int i = 0; i < model.cubes().size(); i++) {
            ModelCube cube = model.cubes().get(i);
            String path = "elements[" + i + "]";

            if (cube == null) {
                addError(issues, path, "Cube is null.");
                continue;
            }

            String uuid = cube.uuid();

            if (uuid == null || uuid.isBlank()) {
                addError(issues, path + ".uuid", "Cube UUID is missing.");
            } else if (!cubeUuids.add(uuid)) {
                addError(
                        issues,
                        path + ".uuid",
                        "Duplicate cube UUID: " + uuid
                );
            }

            if (cube.from() == null || cube.to() == null) {
                addError(
                        issues,
                        path,
                        "Cube has missing geometry."
                );
                continue;
            }

            if (!isFinite(cube.from()) || !isFinite(cube.to())) {
                addError(
                        issues,
                        path,
                        "Cube has non-finite coordinates."
                );
                continue;
            }

            if (cube.from().x() > cube.to().x()
                    || cube.from().y() > cube.to().y()
                    || cube.from().z() > cube.to().z()) {
                addError(
                        issues,
                        path,
                        "Cube has inverted dimensions."
                );
            }

            if (cube.origin() != null && !isFinite(cube.origin())) {
                addError(
                        issues,
                        path + ".origin",
                        "Cube origin contains non-finite coordinates."
                );
            }

            if (cube.rotation() != null && !isFinite(cube.rotation())) {
                addError(
                        issues,
                        path + ".rotation",
                        "Cube rotation contains non-finite values."
                );
            }

            if (!Float.isFinite(cube.inflate())) {
                addError(
                        issues,
                        path + ".inflate",
                        "Cube inflate value is not finite."
                );
            }

            validateFaces(model, cube, path, issues);
        }
    }

    private static void validateFaces(
            BlockbenchModel model,
            ModelCube cube,
            String cubePath,
            List<ValidationIssue> issues
    ) {
        for (var entry : cube.faces().entrySet()) {
            ModelFace.Direction direction = entry.getKey();
            ModelFace face = entry.getValue();

            String path = cubePath + ".faces." +
                    (direction == null
                            ? "unknown"
                            : direction.name().toLowerCase());

            if (direction == null) {
                addError(
                        issues,
                        cubePath + ".faces",
                        "Face has no direction."
                );
                continue;
            }

            if (face == null) {
                addError(issues, path, "Face is null.");
                continue;
            }

            if (face.direction() != direction) {
                addError(
                        issues,
                        path,
                        "Face direction does not match its map key."
                );
            }

            if (face.uv() == null || !isFinite(face.uv())) {
                addError(
                        issues,
                        path + ".uv",
                        "Face has invalid UV coordinates."
                );
            }

            int rotation = face.rotation();

            if (rotation != 0 && rotation != 90
                    && rotation != 180 && rotation != 270) {
                addError(
                        issues,
                        path + ".rotation",
                        "Unsupported UV rotation: " + rotation
                );
            }

            if (face.texture() < -1
                    || face.texture() >= model.textures().size()) {
                addError(
                        issues,
                        path + ".texture",
                        "Invalid texture index: " + face.texture()
                );
            }

            if (face.texture() == -1) {
                addWarning(
                        issues,
                        path + ".texture",
                        "Face has no texture assigned."
                );
            }
        }
    }

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
                addError(
                        issues,
                        path + ".id",
                        "Duplicate texture ID: " + id
                );
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

            if (texture.width() > 0
                    && texture.height() > 0
                    && (image.getWidth() != texture.width()
                    || image.getHeight() != texture.height())) {
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

    private static void validateOutliner(
            BlockbenchModel model,
            List<ValidationIssue> issues
    ) {
        Set<String> knownCubeUuids = new HashSet<>();

        for (ModelCube cube : model.cubes()) {
            if (cube != null && cube.uuid() != null
                    && !cube.uuid().isBlank()) {
                knownCubeUuids.add(cube.uuid());
            }
        }

        Set<String> referencedCubeUuids = new HashSet<>();
        Set<String> nodeUuids = new HashSet<>();

        validateOutlinerNodes(
                model.outliner(),
                "outliner",
                knownCubeUuids,
                referencedCubeUuids,
                nodeUuids,
                issues
        );

        for (String uuid : knownCubeUuids) {
            if (!referencedCubeUuids.contains(uuid)) {
                addError(
                        issues,
                        "outliner",
                        "Cube is missing from outliner: " + uuid
                );
            }
        }
    }

    private static void validateOutlinerNodes(
            List<ModelOutlinerNode> nodes,
            String parentPath,
            Set<String> knownCubeUuids,
            Set<String> referencedCubeUuids,
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

            if (node.uuid() != null && !node.uuid().isBlank()) {
                if (!nodeUuids.add(node.uuid())) {
                    addError(
                            issues,
                            path + ".uuid",
                            "Duplicate outliner node UUID: " + node.uuid()
                    );
                }
            }

            if (node.type() == null) {
                addError(
                        issues,
                        path + ".type",
                        "Outliner node has no type."
                );
            } else if (node.type() == ModelOutlinerNode.Type.CUBE) {
                String uuid = node.cubeUuid();

                if (uuid == null || uuid.isBlank()) {
                    addError(
                            issues,
                            path + ".uuid",
                            "Outliner cube node has no cube UUID."
                    );
                } else if (!knownCubeUuids.contains(uuid)) {
                    addError(
                            issues,
                            path + ".uuid",
                            "Outliner references unknown cube: " + uuid
                    );
                } else if (!referencedCubeUuids.add(uuid)) {
                    addError(
                            issues,
                            path + ".uuid",
                            "Cube appears more than once in outliner: " + uuid
                    );
                }
            }
            else {
                addWarning(
                        issues,
                        path + ".uuid",
                        "Outliner node has no UUID."
                );
            }

            validateOutlinerNodes(
                    node.children(),
                    path + ".children",
                    knownCubeUuids,
                    referencedCubeUuids,
                    nodeUuids,
                    issues
            );
        }
    }

    private static boolean isFinite(ModelVector3 vector) {
        return Float.isFinite(vector.x())
                && Float.isFinite(vector.y())
                && Float.isFinite(vector.z());
    }

    private static boolean isFinite(ModelFace.ModelUv uv) {
        return Float.isFinite(uv.u1())
                && Float.isFinite(uv.v1())
                && Float.isFinite(uv.u2())
                && Float.isFinite(uv.v2());
    }

    private static void addError(
            List<ValidationIssue> issues,
            String path,
            String message
    ) {
        issues.add(new ValidationIssue(
                ValidationSeverity.ERROR,
                path,
                message
        ));
    }

    private static void addWarning(
            List<ValidationIssue> issues,
            String path,
            String message
    ) {
        issues.add(new ValidationIssue(
                ValidationSeverity.WARNING,
                path,
                message
        ));
    }
}