package com.hod.blockbench.loader;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.hod.blockbench.model.*;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class BlockbenchModelParser {

    private BlockbenchModelParser() {}

    static BlockbenchModel parse(
        Reader reader
    ) throws BlockbenchModelException {
        try {
            JsonElement element = JsonParser.parseReader(reader);

            if (!element.isJsonObject()) {
                throw new BlockbenchModelException(
                    "The Blockbench model root must be a JSON object."
                );
            }

            return parseModel(element.getAsJsonObject());
        } catch (JsonParseException | IllegalStateException exception) {
            throw new BlockbenchModelException(
                "Failed to parse Blockbench model JSON.",
                exception
            );
        }
    }

    private static BlockbenchModel parseModel(
        JsonObject root
    ) throws BlockbenchModelException {
        JsonObject meta = object(root, "meta");
        String modelFormat = string(meta, "model_format", "");

        if (!modelFormat.equals("free")) {
            throw new BlockbenchModelException(
                "Unsupported Blockbench model format: " + modelFormat
            );
        }

        JsonObject resolution = object(root, "resolution");

        ModelMetadata metadata = new ModelMetadata(
            string(meta, "format_version", ""),
            modelFormat,
            bool(meta, "box_uv", false),
            integer(resolution, "width", 16),
            integer(resolution, "height", 16)
        );

        List<ModelElement> elements = parseElements(array(root, "elements"));

        List<ModelTexture> textures = parseTextures(array(root, "textures"));

        List<ModelAnimation> animations = parseAnimations(
            array(root, "animations")
        );

        Set<String> elementIds = new HashSet<>();

        for (ModelElement element : elements) {
            if (element.uuid() == null || element.uuid().isBlank()) {
                throw new BlockbenchModelException(
                    "An element is missing its UUID."
                );
            }

            if (!elementIds.add(element.uuid())) {
                throw new BlockbenchModelException(
                    "Duplicate element UUID: " + element.uuid()
                );
            }
        }

        List<ModelOutlinerNode> outliner = parseOutliner(
            array(root, "outliner"),
            array(root, "groups"),
            elements
        );

        return new BlockbenchModel(
            string(root, "name", "Unnamed Model"),
            string(root, "model_identifier", ""),
            metadata,
            elements,
            outliner,
            textures,
            animations,
            root.toString()
        );
    }

    // ---------------------------------------------------------------------
    // Elements
    // ---------------------------------------------------------------------

    private static List<ModelElement> parseElements(
        JsonArray source
    ) throws BlockbenchModelException {
        List<ModelElement> elements = new ArrayList<>();

        for (JsonElement element : source) {
            if (!element.isJsonObject()) {
                continue;
            }

            elements.add(parseElement(element.getAsJsonObject()));
        }

        return List.copyOf(elements);
    }

    private static ModelElement parseElement(
        JsonObject source
    ) throws BlockbenchModelException {
        String uuid = string(source, "uuid", "");

        if (uuid.isBlank()) {
            throw new BlockbenchModelException(
                "An element is missing its UUID."
            );
        }

        String name = string(source, "name", uuid);
        String originalType = string(source, "type", "");

        ModelVector3 origin = vector(source, "origin");
        ModelVector3 rotation = vector(source, "rotation");
        boolean visible = bool(source, "visibility", true);

        /*
         * Mesh geometry takes precedence over cube geometry.
         *
         * This also handles empty mesh elements: an empty vertices/faces
         * object is still a valid mesh element and must be preserved.
         */
        if (hasMeshStructure(source)) {
            return parseMeshElement(
                source,
                uuid,
                name,
                originalType,
                visible,
                origin,
                rotation
            );
        }

        /*
         * Legacy cube elements are converted into the same polygon model.
         */
        if (hasCubeStructure(source)) {
            return parseCubeElement(
                source,
                uuid,
                name,
                originalType,
                visible,
                origin,
                rotation
            );
        }

        /*
         * Preserve unknown or unsupported elements instead of dropping
         * them. They remain available through their raw JSON.
         */
        return new ModelElement(
            uuid,
            name,
            originalType,
            visible,
            origin,
            rotation,
            Map.of(),
            List.of(),
            source.toString()
        );
    }

    private static boolean hasMeshStructure(JsonObject element) {
        return (
            element.has("vertices") &&
            element.get("vertices").isJsonObject() &&
            element.has("faces") &&
            element.get("faces").isJsonObject()
        );
    }

    private static boolean hasCubeStructure(JsonObject element) {
        return (
            element.has("from") &&
            element.get("from").isJsonArray() &&
            element.has("to") &&
            element.get("to").isJsonArray() &&
            element.has("faces") &&
            element.get("faces").isJsonObject()
        );
    }

    private static ModelElement parseMeshElement(
        JsonObject source,
        String uuid,
        String name,
        String originalType,
        boolean visible,
        ModelVector3 origin,
        ModelVector3 rotation
    ) throws BlockbenchModelException {
        Map<String, ModelVector3> vertices = new LinkedHashMap<>();

        JsonObject vertexObject = object(source, "vertices");

        for (Map.Entry<String, JsonElement> entry : vertexObject.entrySet()) {
            String vertexUuid = entry.getKey();
            JsonElement value = entry.getValue();

            if (!value.isJsonArray()) {
                throw new BlockbenchModelException(
                    "Invalid vertex " + vertexUuid + " in element " + uuid
                );
            }

            JsonArray position = value.getAsJsonArray();

            if (position.size() != 3) {
                throw new BlockbenchModelException(
                    "Invalid vertex coordinates for " +
                        vertexUuid +
                        " in element " +
                        uuid
                );
            }

            vertices.put(
                vertexUuid,
                new ModelVector3(
                    position.get(0).getAsFloat(),
                    position.get(1).getAsFloat(),
                    position.get(2).getAsFloat()
                )
            );
        }

        List<ModelPolygonFace> faces = new ArrayList<>();
        JsonObject faceObject = object(source, "faces");

        for (Map.Entry<String, JsonElement> entry : faceObject.entrySet()) {
            String faceUuid = entry.getKey();

            if (!entry.getValue().isJsonObject()) {
                throw new BlockbenchModelException(
                    "Invalid face " + faceUuid + " in element " + uuid
                );
            }

            JsonObject face = entry.getValue().getAsJsonObject();
            JsonArray faceVertices = array(face, "vertices");

            if (faceVertices.size() < 3) {
                throw new BlockbenchModelException(
                    "Face " +
                        faceUuid +
                        " in element " +
                        uuid +
                        " has fewer than three vertices."
                );
            }

            JsonObject uvObject = object(face, "uv");
            List<ModelFaceVertex> corners = new ArrayList<>();

            for (JsonElement vertex : faceVertices) {
                String vertexUuid = vertex.getAsString();

                if (!vertices.containsKey(vertexUuid)) {
                    throw new BlockbenchModelException(
                        "Face " +
                            faceUuid +
                            " in element " +
                            uuid +
                            " references missing vertex " +
                            vertexUuid
                    );
                }

                if (
                    !uvObject.has(vertexUuid) ||
                    !uvObject.get(vertexUuid).isJsonArray()
                ) {
                    throw new BlockbenchModelException(
                        "Face " +
                            faceUuid +
                            " in element " +
                            uuid +
                            " is missing UV coordinates for vertex " +
                            vertexUuid
                    );
                }

                JsonArray coordinates = uvObject
                    .get(vertexUuid)
                    .getAsJsonArray();

                if (coordinates.size() != 2) {
                    throw new BlockbenchModelException(
                        "Invalid UV coordinates for vertex " +
                            vertexUuid +
                            " in face " +
                            faceUuid
                    );
                }

                corners.add(
                    new ModelFaceVertex(
                        vertexUuid,
                        coordinates.get(0).getAsFloat(),
                        coordinates.get(1).getAsFloat()
                    )
                );
            }

            faces.add(
                new ModelPolygonFace(
                    faceUuid,
                    integer(face, "texture", -1),
                    corners
                )
            );
        }

        return new ModelElement(
            uuid,
            name,
            originalType,
            visible,
            origin,
            rotation,
            vertices,
            faces,
            source.toString()
        );
    }

    // ---------------------------------------------------------------------
    // Legacy cube conversion
    // ---------------------------------------------------------------------

    private enum CubeDirection {
        NORTH,
        EAST,
        SOUTH,
        WEST,
        UP,
        DOWN,
    }

    private record CubeFaceDefinition(
        CubeDirection direction,
        int[] cornerIndices,
        int[] uvIndices
    ) {}

    private static final List<CubeFaceDefinition> CUBE_FACE_DEFINITIONS =
        List.of(
            new CubeFaceDefinition(
                CubeDirection.NORTH,
                new int[] { 0, 1, 2, 3 },
                new int[] { 1, 0, 3, 2 }
            ),
            new CubeFaceDefinition(
                CubeDirection.EAST,
                new int[] { 4, 5, 6, 7 },
                new int[] { 0, 1, 2, 3 }
            ),
            new CubeFaceDefinition(
                CubeDirection.SOUTH,
                new int[] { 8, 9, 10, 11 },
                new int[] { 1, 0, 3, 2 }
            ),
            new CubeFaceDefinition(
                CubeDirection.WEST,
                new int[] { 12, 13, 14, 15 },
                new int[] { 0, 1, 2, 3 }
            ),
            new CubeFaceDefinition(
                CubeDirection.UP,
                new int[] { 16, 17, 18, 19 },
                new int[] { 0, 3, 2, 1 }
            ),
            new CubeFaceDefinition(
                CubeDirection.DOWN,
                new int[] { 20, 21, 22, 23 },
                new int[] { 0, 3, 2, 1 }
            )
        );

    private static ModelElement parseCubeElement(
        JsonObject source,
        String uuid,
        String name,
        String originalType,
        boolean visible,
        ModelVector3 origin,
        ModelVector3 rotation
    ) throws BlockbenchModelException {
        ModelVector3 from = vector(source, "from");
        ModelVector3 to = vector(source, "to");

        float inflate = decimal(source, "inflate", 0);

        float x1 = from.x() - inflate;
        float y1 = from.y() - inflate;
        float z1 = from.z() - inflate;

        float x2 = to.x() + inflate;
        float y2 = to.y() + inflate;
        float z2 = to.z() + inflate;

        /*
         * Eight unique cube corners.
         *
         * The face corner order below follows the existing cube mesher's
         * winding convention.
         */
        Map<String, ModelVector3> vertices = new LinkedHashMap<>();

        putVertex(vertices, uuid, 0, x1, y2, z1);
        putVertex(vertices, uuid, 1, x2, y2, z1);
        putVertex(vertices, uuid, 2, x2, y1, z1);
        putVertex(vertices, uuid, 3, x1, y1, z1);

        putVertex(vertices, uuid, 4, x2, y2, z2);
        putVertex(vertices, uuid, 5, x1, y2, z2);
        putVertex(vertices, uuid, 6, x1, y1, z2);
        putVertex(vertices, uuid, 7, x2, y1, z2);

        /*
         * The face definitions use four indices into the ordered 24-corner
         * table below. Each face has its own UV mapping even though cube
         * corners are shared.
         */
        int[][] faceCorners = {
            { 0, 1, 2, 3 }, // NORTH
            { 1, 4, 7, 2 }, // EAST
            { 4, 5, 6, 7 }, // SOUTH
            { 5, 0, 3, 6 }, // WEST
            { 0, 5, 4, 1 }, // UP
            { 6, 3, 2, 7 }, // DOWN
        };

        JsonObject faceObject = object(source, "faces");
        List<ModelPolygonFace> faces = new ArrayList<>();

        for (
            int faceIndex = 0;
            faceIndex < CUBE_FACE_DEFINITIONS.size();
            faceIndex++
        ) {
            CubeFaceDefinition definition = CUBE_FACE_DEFINITIONS.get(
                faceIndex
            );

            String key = definition.direction().name().toLowerCase();

            if (!faceObject.has(key) || !faceObject.get(key).isJsonObject()) {
                continue;
            }

            JsonObject face = faceObject.getAsJsonObject(key);
            JsonArray uv = array(face, "uv");

            if (uv.size() != 4) {
                throw new BlockbenchModelException(
                    "Invalid UV coordinates on cube " + uuid
                );
            }

            float u1 = uv.get(0).getAsFloat();
            float v1 = uv.get(1).getAsFloat();
            float u2 = uv.get(2).getAsFloat();
            float v2 = uv.get(3).getAsFloat();

            List<float[]> uvCorners = List.of(
                new float[] { u1, v1 },
                new float[] { u2, v1 },
                new float[] { u2, v2 },
                new float[] { u1, v2 }
            );

            int rotationSteps = integer(face, "rotation", 0) / 90;
            rotationSteps = Math.floorMod(rotationSteps, 4);

            List<float[]> rotatedUv = new ArrayList<>(4);

            for (int i = 0; i < 4; i++) {
                rotatedUv.add(uvCorners.get((i + rotationSteps) % 4));
            }

            List<ModelFaceVertex> corners = new ArrayList<>(4);

            for (int i = 0; i < 4; i++) {
                int vertexIndex = faceCorners[faceIndex][i];
                String vertexUuid = cubeVertexUuid(uuid, vertexIndex);

                int uvIndex = definition.uvIndices()[i];
                float[] coordinates = rotatedUv.get(uvIndex);

                corners.add(
                    new ModelFaceVertex(
                        vertexUuid,
                        coordinates[0],
                        coordinates[1]
                    )
                );
            }

            faces.add(
                new ModelPolygonFace(
                    uuid + ":face:" + key,
                    integer(face, "texture", -1),
                    corners
                )
            );
        }

        return new ModelElement(
            uuid,
            name,
            originalType.isBlank() ? "cube" : originalType,
            visible,
            origin,
            rotation,
            vertices,
            faces,
            source.toString()
        );
    }

    private static void putVertex(
        Map<String, ModelVector3> vertices,
        String elementUuid,
        int index,
        float x,
        float y,
        float z
    ) {
        vertices.put(
            cubeVertexUuid(elementUuid, index),
            new ModelVector3(x, y, z)
        );
    }

    private static String cubeVertexUuid(String elementUuid, int index) {
        return elementUuid + ":corner:" + index;
    }

    // ---------------------------------------------------------------------
    // Animations
    // ---------------------------------------------------------------------

    private static List<ModelAnimation> parseAnimations(
        JsonArray elements
    ) throws BlockbenchModelException {
        List<ModelAnimation> animations = new ArrayList<>();
        Set<String> animationIds = new HashSet<>();

        for (JsonElement element : elements) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject animation = element.getAsJsonObject();
            String uuid = string(animation, "uuid", "");

            if (uuid.isBlank()) {
                throw new BlockbenchModelException(
                    "An animation is missing its UUID."
                );
            }

            if (!animationIds.add(uuid)) {
                throw new BlockbenchModelException(
                    "Duplicate animation UUID: " + uuid
                );
            }

            animations.add(
                new ModelAnimation(
                    uuid,
                    string(animation, "name", ""),
                    string(animation, "loop", "once"),
                    bool(animation, "override", false),
                    decimal(animation, "length", 0),
                    integer(animation, "snapping", 0),
                    bool(animation, "selected", false),
                    string(animation, "group_name", ""),
                    integer(animation, "scope", 0),
                    string(animation, "anim_time_update", ""),
                    string(animation, "blend_weight", ""),
                    string(animation, "start_delay", ""),
                    string(animation, "loop_delay", ""),
                    parseAnimators(object(animation, "animators"))
                )
            );
        }

        return List.copyOf(animations);
    }

    private static Map<String, ModelAnimator> parseAnimators(
        JsonObject animators
    ) throws BlockbenchModelException {
        Map<String, ModelAnimator> result = new LinkedHashMap<>();

        for (Map.Entry<String, JsonElement> entry : animators.entrySet()) {
            String targetUuid = entry.getKey();

            if (!entry.getValue().isJsonObject()) {
                continue;
            }

            JsonObject animator = entry.getValue().getAsJsonObject();

            if (targetUuid.isBlank()) {
                throw new BlockbenchModelException(
                    "An animator is missing its target UUID."
                );
            }

            result.put(
                targetUuid,
                new ModelAnimator(
                    targetUuid,
                    string(animator, "name", ""),
                    string(animator, "type", ""),
                    bool(animator, "rotation_global", false),
                    bool(animator, "quaternion_interpolation", false),
                    parseKeyframes(array(animator, "keyframes"))
                )
            );
        }

        return result;
    }

    private static List<AnimationKeyframe> parseKeyframes(
        JsonArray keyframes
    ) throws BlockbenchModelException {
        List<AnimationKeyframe> result = new ArrayList<>();
        Set<String> keyframeIds = new HashSet<>();

        for (JsonElement element : keyframes) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject keyframe = element.getAsJsonObject();
            String uuid = string(keyframe, "uuid", "");

            if (uuid.isBlank()) {
                throw new BlockbenchModelException(
                    "An animation keyframe is missing its UUID."
                );
            }

            if (!keyframeIds.add(uuid)) {
                throw new BlockbenchModelException(
                    "Duplicate animation keyframe UUID: " + uuid
                );
            }

            result.add(
                new AnimationKeyframe(
                    uuid,
                    string(keyframe, "channel", ""),
                    decimal(keyframe, "time", 0),
                    string(keyframe, "interpolation", "linear"),
                    integer(keyframe, "color", -1),
                    parseAnimationDataPoints(array(keyframe, "data_points"))
                )
            );
        }

        return List.copyOf(result);
    }

    private static List<AnimationDataPoint> parseAnimationDataPoints(
        JsonArray dataPoints
    ) {
        List<AnimationDataPoint> result = new ArrayList<>();

        for (JsonElement element : dataPoints) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject dataPoint = element.getAsJsonObject();

            result.add(
                new AnimationDataPoint(
                    string(dataPoint, "x", "0"),
                    string(dataPoint, "y", "0"),
                    string(dataPoint, "z", "0")
                )
            );
        }

        return List.copyOf(result);
    }

    // ---------------------------------------------------------------------
    // Textures
    // ---------------------------------------------------------------------

    private static String normalizeTexturePath(
        String path,
        String relativePath,
        String folder
    ) {
        if (path != null && !path.isBlank()) {
            return path;
        }

        if (relativePath == null || relativePath.isBlank()) {
            return "";
        }

        String normalized = relativePath.replace('\\', '/');
        String texturePrefix = "../textures/";

        if (normalized.startsWith(texturePrefix)) {
            normalized = normalized.substring(texturePrefix.length());
        }

        if (normalized.startsWith("textures/")) {
            normalized = normalized.substring("textures/".length());
        }

        if (normalized.startsWith("../") || normalized.startsWith("/")) {
            throw new IllegalArgumentException(
                "Invalid relative texture path: " + relativePath
            );
        }

        if (
            normalized.indexOf('/') < 0 && folder != null && !folder.isBlank()
        ) {
            normalized = folder + "/" + normalized;
        }

        return normalized;
    }

    private static List<ModelTexture> parseTextures(
        JsonArray elements
    ) throws BlockbenchModelException {
        List<ModelTexture> textures = new ArrayList<>();

        for (JsonElement element : elements) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject texture = element.getAsJsonObject();
            String source = string(texture, "source", "");
            byte[] imageData = decodeTexture(source);

            textures.add(
                new ModelTexture(
                    string(texture, "id", ""),
                    string(texture, "name", ""),
                    integer(texture, "width", 0),
                    integer(texture, "height", 0),
                    integer(texture, "uv_width", 0),
                    integer(texture, "uv_height", 0),
                    integer(texture, "frame_time", 1),
                    string(texture, "frame_order_type", ""),
                    string(texture, "frame_order", ""),
                    bool(texture, "frame_interpolate", false),
                    source,
                    imageData,
                    normalizeTexturePath(
                        string(texture, "path", ""),
                        string(texture, "relative_path", ""),
                        string(texture, "folder", "")
                    ),
                    string(texture, "folder", ""),
                    string(texture, "namespace", ""),
                    bool(texture, "internal", false),
                    bool(texture, "saved", false)
                )
            );
        }

        return List.copyOf(textures);
    }

    private static byte[] decodeTexture(
        String source
    ) throws BlockbenchModelException {
        if (source == null || source.isBlank()) {
            return null;
        }

        String prefix = "base64,";
        int separator = source.indexOf(prefix);

        if (separator < 0) {
            return null;
        }

        try {
            return Base64.getDecoder().decode(
                source.substring(separator + prefix.length())
            );
        } catch (IllegalArgumentException exception) {
            throw new BlockbenchModelException(
                "Invalid embedded texture data.",
                exception
            );
        }
    }

    // ---------------------------------------------------------------------
    // Outliner
    // ---------------------------------------------------------------------

    private static List<ModelOutlinerNode> parseOutliner(
        JsonArray outliner,
        JsonArray groups,
        List<ModelElement> elements
    ) throws BlockbenchModelException {
        Map<String, JsonObject> groupDefinitions = new HashMap<>();
        Map<String, ModelElement> elementDefinitions = new HashMap<>();

        for (ModelElement element : elements) {
            elementDefinitions.put(element.uuid(), element);
        }

        for (JsonElement element : groups) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject group = element.getAsJsonObject();
            String uuid = string(group, "uuid", "");

            if (!uuid.isBlank()) {
                groupDefinitions.put(uuid, group);
            }
        }

        indexGroups(outliner, groupDefinitions);

        List<ModelOutlinerNode> result = new ArrayList<>();
        Set<String> resolving = new HashSet<>();

        for (JsonElement element : outliner) {
            result.add(
                resolveNode(
                    element,
                    groupDefinitions,
                    elementDefinitions,
                    resolving
                )
            );
        }

        return List.copyOf(result);
    }

    private static void indexGroups(
        JsonArray elements,
        Map<String, JsonObject> definitions
    ) {
        for (JsonElement element : elements) {
            if (!element.isJsonObject()) {
                continue;
            }

            JsonObject object = element.getAsJsonObject();
            String uuid = string(object, "uuid", "");

            if (!uuid.isBlank()) {
                definitions.putIfAbsent(uuid, object);
            }

            indexGroups(array(object, "children"), definitions);
        }
    }

    private static ModelOutlinerNode resolveNode(
        JsonElement element,
        Map<String, JsonObject> groups,
        Map<String, ModelElement> elements,
        Set<String> resolving
    ) throws BlockbenchModelException {
        if (
            element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()
        ) {
            String uuid = element.getAsString();
            ModelElement modelElement = elements.get(uuid);

            if (modelElement != null) {
                return new ModelOutlinerNode(
                    uuid,
                    modelElement.name(),
                    ModelOutlinerNode.Type.ELEMENT,
                    ModelVector3.ZERO,
                    ModelVector3.ZERO,
                    uuid,
                    List.of(),
                    element.toString(),
                    modelElement.visible(),
                    true,
                    false,
                    -1,
                    true,
                    true,
                    false,
                    0
                );
            }

            JsonObject group = groups.get(uuid);

            if (group != null) {
                return resolveGroup(group, groups, elements, resolving);
            }

            return new ModelOutlinerNode(
                uuid,
                uuid,
                ModelOutlinerNode.Type.OTHER,
                ModelVector3.ZERO,
                ModelVector3.ZERO,
                null,
                List.of(),
                element.toString(),
                true,
                true,
                false,
                -1,
                true,
                true,
                false,
                0
            );
        }

        if (element.isJsonObject()) {
            JsonObject occurrence = element.getAsJsonObject();
            String uuid = string(occurrence, "uuid", "");
            JsonObject definition = groups.get(uuid);

            if (definition != null && definition != occurrence) {
                JsonObject merged = definition.deepCopy();

                for (Map.Entry<
                    String,
                    JsonElement
                > entry : occurrence.entrySet()) {
                    merged.add(entry.getKey(), entry.getValue().deepCopy());
                }

                return resolveGroup(merged, groups, elements, resolving);
            }

            return resolveGroup(occurrence, groups, elements, resolving);
        }

        return new ModelOutlinerNode(
            "",
            "",
            ModelOutlinerNode.Type.OTHER,
            ModelVector3.ZERO,
            ModelVector3.ZERO,
            null,
            List.of(),
            element.toString(),
            true,
            true,
            false,
            -1,
            true,
            true,
            false,
            0
        );
    }

    private static ModelOutlinerNode resolveGroup(
        JsonObject group,
        Map<String, JsonObject> groups,
        Map<String, ModelElement> elements,
        Set<String> resolving
    ) throws BlockbenchModelException {
        String uuid = string(group, "uuid", "");
        boolean tracked = !uuid.isBlank();

        if (tracked && !resolving.add(uuid)) {
            throw new BlockbenchModelException(
                "Circular outliner reference: " + uuid
            );
        }

        try {
            List<ModelOutlinerNode> children = new ArrayList<>();

            for (JsonElement child : array(group, "children")) {
                children.add(resolveNode(child, groups, elements, resolving));
            }

            return new ModelOutlinerNode(
                uuid,
                string(group, "name", ""),
                ModelOutlinerNode.Type.GROUP,
                vector(group, "origin"),
                vector(group, "rotation"),
                null,
                children,
                group.toString(),
                bool(group, "visibility", true),
                bool(group, "export", true),
                bool(group, "locked", false),
                integer(group, "color", -1),
                bool(group, "isOpen", true),
                bool(group, "shade", true),
                bool(group, "mirror_uv", false),
                integer(group, "autouv", 0)
            );
        } finally {
            if (tracked) {
                resolving.remove(uuid);
            }
        }
    }

    // ---------------------------------------------------------------------
    // JSON helpers
    // ---------------------------------------------------------------------

    private static JsonObject object(JsonObject parent, String key) {
        if (parent.has(key) && parent.get(key).isJsonObject()) {
            return parent.getAsJsonObject(key);
        }

        return new JsonObject();
    }

    private static JsonArray array(JsonObject parent, String key) {
        if (parent.has(key) && parent.get(key).isJsonArray()) {
            return parent.getAsJsonArray(key);
        }

        return new JsonArray();
    }

    private static String string(
        JsonObject object,
        String key,
        String fallback
    ) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }

        return object.get(key).getAsString();
    }

    private static boolean bool(
        JsonObject object,
        String key,
        boolean fallback
    ) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }

        return object.get(key).getAsBoolean();
    }

    private static int integer(JsonObject object, String key, int fallback) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }

        return object.get(key).getAsInt();
    }

    private static float decimal(
        JsonObject object,
        String key,
        float fallback
    ) {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return fallback;
        }

        return object.get(key).getAsFloat();
    }

    private static ModelVector3 vector(
        JsonObject object,
        String key
    ) throws BlockbenchModelException {
        JsonArray values = array(object, key);

        if (values.isEmpty()) {
            return ModelVector3.ZERO;
        }

        if (values.size() != 3) {
            throw new BlockbenchModelException("Invalid vector: " + key);
        }

        return new ModelVector3(
            values.get(0).getAsFloat(),
            values.get(1).getAsFloat(),
            values.get(2).getAsFloat()
        );
    }
}
